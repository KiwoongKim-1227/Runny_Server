package com.goodspace.runny.global.util;

import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * S3 이미지 업로드/삭제 공용 유틸. 확장자(jpg/jpeg/png/webp)와 10MB 크기를 검증하고
 * UUID 파일명으로 저장한다(원본 파일명 미저장). key 프리픽스(crew/, route/ 등)로 용도를 구분한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    /** 이미지 업로드 후 공개 읽기 URL 반환. prefix 예: "crew/", "route/" */
    public String upload(MultipartFile file, String prefix) {
        validate(file);
        String extension = extractExtension(file.getOriginalFilename());
        String key = prefix + UUID.randomUUID() + "." + extension;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_003);
        } catch (Exception e) {
            log.error("S3 업로드 실패: key={}", key, e);
            throw new ExternalApiException("S3 업로드에 실패했습니다.");
        }
        return publicUrl(key);
    }

    /** 단건 삭제 (URL 또는 key 모두 허용). 실패해도 서비스 흐름은 계속 - 고아 객체는 배치 정리 대상 */
    public void delete(String urlOrKey) {
        String key = toKey(urlOrKey);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("S3 삭제 실패(무시하고 진행): key={}", key, e);
        }
    }

    /** 프리픽스 하위 객체 일괄 삭제 (예: 회원 탈퇴 시 해당 유저 route/ 이미지 정리) */
    public void deleteAll(String prefix) {
        try {
            String continuationToken = null;
            do {
                ListObjectsV2Response listed = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .continuationToken(continuationToken)
                        .build());
                List<ObjectIdentifier> targets = listed.contents().stream()
                        .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                        .toList();
                if (!targets.isEmpty()) {
                    s3Client.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(targets).build())
                            .build());
                }
                continuationToken = listed.isTruncated() ? listed.nextContinuationToken() : null;
            } while (continuationToken != null);
        } catch (Exception e) {
            log.warn("S3 일괄 삭제 실패(무시하고 진행): prefix={}", prefix, e);
        }
    }

    /** 확장자/크기/Content-Type 검증 */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_001, "업로드할 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_002);
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.IMAGE_001);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ErrorCode.IMAGE_001);
        }
    }

    /** 파일명에서 소문자 확장자 추출 */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.IMAGE_001);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** 공개 읽기 URL 생성 */
    private String publicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    /** URL이 들어와도 key로 변환 */
    private String toKey(String urlOrKey) {
        String marker = ".amazonaws.com/";
        int idx = urlOrKey.indexOf(marker);
        return idx >= 0 ? urlOrKey.substring(idx + marker.length()) : urlOrKey;
    }
}
