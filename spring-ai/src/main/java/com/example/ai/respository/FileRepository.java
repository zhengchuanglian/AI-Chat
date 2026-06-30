package com.example.ai.respository;

import org.springframework.core.io.Resource;

public interface FileRepository {

    //保存文件，记录id
    boolean save(String chatId, Resource  resource);


    //根据chatId获取文件
    Resource getFile(String chatId);
}
