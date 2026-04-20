package edge.verdant.utils;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.Protocol;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyuncs.exceptions.ClientException;
import edge.verdant.properties.AliyunOSSProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 阿里云 OSS 操作类
 * @author Ling_Yu175
 */

@Component
public class AliyunOSSOperator {

    // 创建 ClientBuilderConfiguration 实例，用于配置 OSS 客户端参数
    private static final ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
    static {
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);// 设置签名算法版本为 V4
        clientBuilderConfiguration.setProtocol(Protocol.HTTPS);// 设置使用 HTTPS 协议访问 OSS，保证传输安全性
    }

    @Autowired
    private AliyunOSSProperties aliyunOSSProperties;

    public AliyunOSSOperator() {

    }

    public AliyunOSSOperator(AliyunOSSProperties aliyunOSSProperties) {
        this.aliyunOSSProperties = aliyunOSSProperties;
    }

    /**
     * 自定义 MultipartFile 实现，用于包装 OSS 文件数据
     */
    private static class OSSMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public OSSMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getOriginalFilename() {
            return this.originalFilename;
        }

        @Override
        public String getContentType() {
            return this.contentType;
        }

        @Override
        public boolean isEmpty() {
            return this.content.length == 0;
        }

        @Override
        public long getSize() {
            return this.content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return this.content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(this.content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            try (InputStream in = getInputStream();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    /**
     * 上传文件
     * @param file 文件
     * @return 文件访问路径
     */
    public String upload(byte[] file, String fileName) {
        OSS ossClient = null;
        try {
            ossClient = createOSS();// 1 创建 OSS 客户端实例
            InputStream inputStream = new ByteArrayInputStream(file);
            ossClient.putObject(aliyunOSSProperties.getBucketName(), fileName, inputStream);// 3 上传 文件
        }catch (ClientException e) {
            return "FAIL";// 4 返回 文件访问路径
        }
        finally {
            if (ossClient != null) ossClient.shutdown();// 当OSSClient实例不再使用时，调用shutdown方法以释放资源
        }
        return getURL(fileName);// 4 返回 文件访问路径
    }

    /**
     * 创建 OSS 客户端实例
     * @return OSS 客户端实例
     * @throws ClientException 创建 OSS 客户端实例失败
     */
    public OSS createOSS() throws ClientException {
        return OSSClientBuilder.create()
                               // 以华东1（杭州）地域的外网访问域名为例，Endpoint填写为oss-cn-hangzhou.aliyuncs.com
                               .endpoint(aliyunOSSProperties.getEndpoint())
                               .credentialsProvider(new DefaultCredentialProvider(
                                       aliyunOSSProperties.getAccessKeyId(),
                                       aliyunOSSProperties.getAccessKeySecret()
                               ))
                               // 设置客户端配置
                               .clientConfiguration(clientBuilderConfiguration)
                               // 以华东1（杭州）地域为例，Region填写为cn-hangzhou
                               .region(aliyunOSSProperties.getRegion())
                               .build();
    }

    /**
     * 获取当前年月
     * @return 当前年月
     */
    private String getYearAndMonth() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
    }

    /**
     * 获取文件扩展名
     * @param file 文件
     * @return 文件扩展名
     */
    private String getFileExtension(MultipartFile file) {
        return Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));
    }

    /**
     * 获取文件访问路径
     * @param fileName 文件名
     * @return 文件访问路径
     */
    public String getURL(String fileName) {
        return "https://" + aliyunOSSProperties.getBucketName() + "." + aliyunOSSProperties.getEndpoint() + "/" + fileName;
    }
}