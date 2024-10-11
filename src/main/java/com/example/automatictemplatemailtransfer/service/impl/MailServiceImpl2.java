package com.example.automatictemplatemailtransfer.service.impl;

import com.aspose.words.Document;
import com.aspose.words.FindReplaceOptions;
import com.aspose.words.HtmlSaveOptions;
import com.example.automatictemplatemailtransfer.model.response.Response;
import com.example.automatictemplatemailtransfer.model.response.SendMailResponse;
import com.example.automatictemplatemailtransfer.service.MailService;
import com.example.automatictemplatemailtransfer.shared.constant.AsposeAdvertise;
import com.example.automatictemplatemailtransfer.shared.constant.BookmarkName;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl2 implements MailService {
  private final JavaMailSender mailSender;

  @Override
  public Response<SendMailResponse> sendMail(MultipartFile file) throws IOException {
    log.info("=====> After successfully get the csv file");
    if (file.isEmpty()) {
      throw new IllegalArgumentException("File is empty");
    }

    List<List<String>> csvData = this.getCSVData(file);

    log.info("=====> After successfully get the csv data");
    csvData.forEach(rowData -> {
      if (!this.isDefaultRow(rowData)) {
        Document document = null;
        try {
          InputStream fileStream = getClass().getClassLoader().getResourceAsStream("templates/template1.docx");
          document = new Document(fileStream);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        try {
          document.getRange().replace("{{Full_Name}}", rowData.get(1), new FindReplaceOptions());
          document.getRange().replace("{{Current_Title}}", rowData.get(2), new FindReplaceOptions());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        try {
          document.save("apose-html/template.html", this.getHtmlSaveOptions());
        } catch (Exception e) {
          throw new RuntimeException(e);
        }

        String htmlContent = null;
        try {
          htmlContent = this.getHtmlStringAfterRemoveAsposeAdvertisedLogo("apose-html/template.html");
          htmlContent = this.removeAsposeAdvertisement(htmlContent);
//          log.info("=====> html content:{}", htmlContent);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        log.info("=====>About to send mail");
        try {
          this.mailSending(htmlContent);
        } catch (MessagingException e) {
          throw new RuntimeException(e);
        }
      }
//      log.info("HTML content {}:",htmlContent);
    });
    return Response.<SendMailResponse>builder()
            .id(UUID.randomUUID().toString())
            .data(SendMailResponse.builder().success(true).build())
            .build();
  }

  private List<List<String>> getCSVData(MultipartFile file) throws IOException {
    List<List<String>> data = new ArrayList<>();
    Reader reader = new InputStreamReader(file.getInputStream());
    CSVParser csvRecords = new CSVParser(reader, CSVFormat.DEFAULT);
    for (CSVRecord record : csvRecords) {
      List<String> row = new ArrayList<>();
      record.forEach(row::add);
      data.add(row);
    }

    return data;
  }

  private void mailSending(String content) throws MessagingException {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
    mimeMessageHelper.setTo("nbao0208lhp@gmail.com");
    mimeMessageHelper.setSubject("New");
    mimeMessageHelper.setText(content, true);
    mailSender.send(mimeMessage);
  }

  private boolean isDefaultRow(List<String> rowData) {
    return rowData.get(1).equals(BookmarkName.FULL_NAME) || rowData.get(2).equals(BookmarkName.CURRENT_TITLE);
  }

  private HtmlSaveOptions getHtmlSaveOptions() {
    HtmlSaveOptions htmlSaveOptions = new HtmlSaveOptions();
    htmlSaveOptions.setExportImagesAsBase64(true);
    return htmlSaveOptions;
  }

  private String removeAsposeAdvertisement(String htmlContent) {
    for (String adString : AsposeAdvertise.ASPOSE_ADVERTISE) {
      htmlContent = htmlContent.replace(adString, "");
    }
    return htmlContent;
  }

  private String getHtmlStringAfterRemoveAsposeAdvertisedLogo(String htmlPath) throws IOException {
    File htmlFile = new File(htmlPath);
    org.jsoup.nodes.Document htmlDoc = Jsoup.parse(htmlFile,"UTF-8");

    Elements spanElements = htmlDoc.select("span");

    for (Element element : spanElements) {
      String style = element.attr("style");
      if (style.contains("z-index")){
        element.remove();
      }
    }

    return htmlDoc.outerHtml();
  }
}
