package com.gxa.pipe.utils;

import com.aliyun.oss.*;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.gxa.pipe.config.OSSConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * 阿里云OSS工具类
 * 提供文件上传到阿里云OSS的功能
 */
@Component
public class OSSUtils {

    private static final Logger logger = LoggerFactory.getLogger(OSSUtils.class);

    // 默认OSS配置信息（作为备用）
    private static final String DEFAULT_ENDPOINT = "https://oss-cn-beijing.aliyuncs.com";
    private static final String DEFAULT_ACCESS_KEY_ID = "LTAI5tJ4AEtQ3tPqzMGok2gt";
    private static final String DEFAULT_ACCESS_KEY_SECRET = "19HhwdCzjeICeqZexFc8JUxRbJPBkp";
    private static final String DEFAULT_BUCKET_NAME = "zhizyy-bucket1";

    private static OSSConfig ossConfig;

    @Autowired
    public void setOssConfig(OSSConfig ossConfig) {
        OSSUtils.ossConfig = ossConfig;
    }

    /**
     * 生成唯一的文件名
     * 
     * @param originalFilename 原始文件名
     * @return 唯一的文件名
     */
    public static String generateUniqueFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 上传文件到OSS
     * 
     * @param objectName OSS中的对象名称（文件路径）
     * @param filePath   本地文件路径
     * @return 上传是否成功
     */
    public static boolean uploadFile(String objectName, String filePath) {
        OSS ossClient = null;
        try {
            logger.info("开始上传文件到OSS: {}", objectName);

            // 获取配置信息
            String endpoint = getEndpoint();
            String accessKeyId = getAccessKeyId();
            String accessKeySecret = getAccessKeySecret();
            String bucketName = getBucketName();

            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists()) {
                logger.error("文件不存在: {}", filePath);
                return false;
            }

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file);

            // 上传文件
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            logger.info("文件上传成功: {}, ETag: {}", objectName, result.getETag());
            return true;

        } catch (OSSException oe) {
            logger.error("OSS异常 - 错误消息: {}, 错误代码: {}, 请求ID: {}, 主机ID: {}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            return false;
        } catch (ClientException ce) {
            logger.error("客户端异常: {}", ce.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("上传文件时发生异常: {}", e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件到OSS（使用MultipartFile）
     * 
     * @param objectName    OSS中的对象名称（文件路径）
     * @param multipartFile Spring MultipartFile对象
     * @return 上传是否成功
     */
    public static boolean uploadFile(String objectName, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            logger.error("上传文件为空");
            return false;
        }

        try {
            logger.info("开始上传MultipartFile到OSS: {}, 原始文件名: {}", objectName, multipartFile.getOriginalFilename());
            return uploadFile(objectName, multipartFile.getInputStream());
        } catch (IOException e) {
            logger.error("获取MultipartFile输入流时发生异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 上传文件到OSS（使用MultipartFile，自动生成唯一文件名）
     * 
     * @param directory     OSS中的目录路径（如: "images/", "documents/"）
     * @param multipartFile Spring MultipartFile对象
     * @return 上传成功返回完整的对象名称，失败返回null
     */
    public static String uploadFileWithUniqueKey(String directory, MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            logger.error("上传文件为空");
            return null;
        }

        // 生成唯一文件名
        String uniqueFileName = generateUniqueFileName(multipartFile.getOriginalFilename());
        String objectName = directory + (directory.endsWith("/") ? "" : "/") + uniqueFileName;

        boolean success = uploadFile(objectName, multipartFile);
        return success ? objectName : null;
    }

    /**
     * 上传文件到OSS（使用InputStream）
     * 
     * @param objectName  OSS中的对象名称（文件路径）
     * @param inputStream 文件输入流
     * @return 上传是否成功
     */
    public static boolean uploadFile(String objectName, InputStream inputStream) {
        OSS ossClient = null;
        try {
            logger.info("开始上传文件流到OSS: {}", objectName);

            // 获取配置信息
            String endpoint = getEndpoint();
            String accessKeyId = getAccessKeyId();
            String accessKeySecret = getAccessKeySecret();
            String bucketName = getBucketName();

            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);

            // 上传文件
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            logger.info("文件流上传成功: {}, ETag: {}", objectName, result.getETag());
            return true;

        } catch (OSSException oe) {
            logger.error("OSS异常 - 错误消息: {}, 错误代码: {}, 请求ID: {}, 主机ID: {}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            return false;
        } catch (ClientException ce) {
            logger.error("客户端异常: {}", ce.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("上传文件流时发生异常: {}", e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件到OSS（自定义配置）
     * 
     * @param endpoint        OSS端点
     * @param accessKeyId     访问密钥ID
     * @param accessKeySecret 访问密钥Secret
     * @param bucketName      存储桶名称
     * @param objectName      OSS中的对象名称（文件路径）
     * @param filePath        本地文件路径
     * @return 上传是否成功
     */
    public static boolean uploadFile(String endpoint, String accessKeyId, String accessKeySecret,
            String bucketName, String objectName, String filePath) {
        OSS ossClient = null;
        try {
            logger.info("开始使用自定义配置上传文件到OSS: {}", objectName);

            // 创建OSSClient实例
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists()) {
                logger.error("文件不存在: {}", filePath);
                return false;
            }

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, file);

            // 上传文件
            PutObjectResult result = ossClient.putObject(putObjectRequest);
            logger.info("自定义配置文件上传成功: {}, ETag: {}", objectName, result.getETag());
            return true;

        } catch (OSSException oe) {
            logger.error("OSS异常 - 错误消息: {}, 错误代码: {}, 请求ID: {}, 主机ID: {}",
                    oe.getErrorMessage(), oe.getErrorCode(), oe.getRequestId(), oe.getHostId());
            return false;
        } catch (ClientException ce) {
            logger.error("客户端异常: {}", ce.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("上传文件时发生异常: {}", e.getMessage(), e);
            return false;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 获取端点配置
     * 
     * @return 端点URL
     */
    private static String getEndpoint() {
        return ossConfig != null && ossConfig.getEndpoint() != null ? ossConfig.getEndpoint() : DEFAULT_ENDPOINT;
    }

    /**
     * 获取访问密钥ID
     * 
     * @return 访问密钥ID
     */
    private static String getAccessKeyId() {
        return ossConfig != null && ossConfig.getAccessKeyId() != null ? ossConfig.getAccessKeyId()
                : DEFAULT_ACCESS_KEY_ID;
    }

    /**
     * 获取访问密钥Secret
     * 
     * @return 访问密钥Secret
     */
    private static String getAccessKeySecret() {
        return ossConfig != null && ossConfig.getAccessKeySecret() != null ? ossConfig.getAccessKeySecret()
                : DEFAULT_ACCESS_KEY_SECRET;
    }

    /**
     * 获取存储桶名称
     * 
     * @return 存储桶名称
     */
    private static String getBucketName() {
        return ossConfig != null && ossConfig.getBucketName() != null ? ossConfig.getBucketName() : DEFAULT_BUCKET_NAME;
    }

    /**
     * 获取当前使用的存储桶名称
     * 
     * @return 存储桶名称
     */
    public static String getCurrentBucketName() {
        return getBucketName();
    }

    /**
     * 获取当前使用的端点
     * 
     * @return 端点URL
     */
    public static String getCurrentEndpoint() {
        return getEndpoint();
    }

    /**
     * 检查OSS配置是否有效
     * 
     * @return 配置是否有效
     */
    public static boolean isConfigValid() {
        String endpoint = getEndpoint();
        String accessKeyId = getAccessKeyId();
        String accessKeySecret = getAccessKeySecret();
        String bucketName = getBucketName();

        return endpoint != null && !endpoint.trim().isEmpty() &&
                accessKeyId != null && !accessKeyId.trim().isEmpty() &&
                accessKeySecret != null && !accessKeySecret.trim().isEmpty() &&
                bucketName != null && !bucketName.trim().isEmpty();
    }
}