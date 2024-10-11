package com.example.automatictemplatemailtransfer.controller;

import com.example.automatictemplatemailtransfer.model.response.Response;
import com.example.automatictemplatemailtransfer.model.response.SendMailResponse;
import com.example.automatictemplatemailtransfer.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/mail")
@RequiredArgsConstructor
public class MailController {
  private final MailService mailServiceImpl2;

  @GetMapping(value = "/send")
  public Response<SendMailResponse> sendMail(@RequestParam("csv_file") MultipartFile file) throws IOException {
    return mailServiceImpl2.sendMail(file);
  }
}
