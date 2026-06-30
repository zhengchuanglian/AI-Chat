package com.example.ai.respository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Properties;


@Slf4j
@Component
public class LocalPdfFileReposity implements FileRepository{

    private final VectorStore vectorStore;
    private final Properties chatFile=new Properties();

    public LocalPdfFileReposity(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    @Override
    public boolean save(String chatId, Resource resource) {

        //保存本地磁盘
        String fileName=resource.getFilename();
        File target=new File(Objects.requireNonNull(fileName));
        if (!target.exists()){
            try {
                Files.copy(resource.getInputStream(),target.toPath());

            } catch (Exception e) {
                log.error("保存文件失败",e);
                return false;
            }
        }
        //2.保存映射关系
        chatFile.put(chatId,fileName);
        
        //3.加载PDF内容到VectorStore
        try {
            loadPdfToVectorStore(target, fileName);
            log.info("PDF文件已加载到VectorStore: {}", fileName);
        } catch (Exception e) {
            log.error("加载PDF到VectorStore失败", e);
            return false;
        }
        
        return true;

    }
    
    private void loadPdfToVectorStore(File pdfFile, String fileName) {
        try {
            // 使用PagePdfDocumentReader读取PDF内容
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new FileSystemResource(pdfFile));
            List<Document> documents = pdfReader.get();
            
            // 为每个文档添加文件名元数据，用于后续过滤
            for (Document doc : documents) {
                doc.getMetadata().put("file_name", fileName);
            }
            
            // 使用TokenTextSplitter分割文档
            TokenTextSplitter textSplitter = new TokenTextSplitter();
            List<Document> splitDocuments = textSplitter.apply(documents);
            
            // 将分割后的文档添加到VectorStore
            vectorStore.add(splitDocuments);
            
            log.info("成功加载PDF到VectorStore，共 {} 个文档片段", splitDocuments.size());
        } catch (Exception e) {
            log.error("加载PDF到VectorStore失败", e);
            throw new RuntimeException("加载PDF失败", e);
        }
    }

    @Override
    public Resource getFile(String chatId) {
        return new FileSystemResource(chatFile.getProperty(chatId));
    }

    @PostConstruct
    public void init(){
        //初始化 - 加载 chatId 与文件名的映射关系
        FileSystemResource pdfFile=new FileSystemResource("chat-pdf.properties");
        if (pdfFile.exists()){
            try{
                chatFile.load(new BufferedReader(new InputStreamReader(pdfFile.getInputStream())));
                log.info("已加载 {} 个文件映射记录", chatFile.size());
            }catch (IOException  e){
                log.error("加载文件映射失败", e);
                throw new RuntimeException("加载文件失败",e);
            }
        }
        // 注意：Redis VectorStore 不需要手动加载，数据会自动从 Redis 读取
        log.info("VectorStore 初始化完成");
    }

    @PreDestroy
    public void persistent(){
        //保存 chatId 与文件名的映射关系到本地文件
        try(
            FileWriter writer = new FileWriter("chat-pdf.properties")
        ){
            chatFile.store(writer, "Saved at " + LocalDateTime.now());
            log.info("已持久化 {} 个文件映射记录", chatFile.size());
        } catch (IOException e) {
            log.error("持久化文件映射失败", e);
            throw new RuntimeException(e);
        }
        // 注意：Redis VectorStore 会自动同步数据到 Redis，不需要手动保存
    }

}
