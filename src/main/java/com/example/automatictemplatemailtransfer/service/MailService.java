package com.example.automatictemplatemailtransfer.service;

import com.example.automatictemplatemailtransfer.model.response.Response;
import com.example.automatictemplatemailtransfer.model.response.SendMailResponse;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Repository
public interface MailService {
  Response<SendMailResponse> sendMail(MultipartFile file) throws IOException;
}
