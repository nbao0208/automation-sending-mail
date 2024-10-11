package com.example.automatictemplatemailtransfer.service.impl;

import com.example.automatictemplatemailtransfer.model.response.Response;
import com.example.automatictemplatemailtransfer.model.response.SendMailResponse;
import com.example.automatictemplatemailtransfer.service.MailService;
import com.example.automatictemplatemailtransfer.shared.constant.AdvertiseString;
import com.example.automatictemplatemailtransfer.shared.constant.BookmarkName;
import com.spire.doc.Bookmark;
import com.spire.doc.Document;
import com.spire.doc.FileFormat;
import com.spire.doc.documents.Paragraph;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailServiceImpl implements MailService {
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
        Document document = new Document();
        ClassPathResource classPathResource = new ClassPathResource("templates/template.docx");
        try {
          document.loadFromFile(classPathResource.getFile().getAbsolutePath());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        Bookmark fullName = document.getBookmarks().findByName(BookmarkName.FULL_NAME);
        Bookmark currentTitle = document.getBookmarks().findByName(BookmarkName.CURRENT_TITLE);

        Paragraph paragraphContainFullName = fullName.getBookmarkStart().getOwnerParagraph();
        Paragraph paragraphContainCurrentTitle = currentTitle.getBookmarkStart().getOwnerParagraph();
        paragraphContainFullName.appendText(rowData.get(1));
        paragraphContainCurrentTitle.appendText(rowData.get(2));

        document.saveToFile("html/template.html", FileFormat.Html);
        try {
          this.convertImageToBase64("html/template.html");
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        String htmlContent = null;
        try {
          htmlContent = this.getHtmlContent("html/template-base64-img.html");
          log.info("=====> html content:{}", htmlContent);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        log.info("=====>About to send mail");
        try {
          this.mailSending(this.removeAdvertise(htmlContent));
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
    mimeMessageHelper.setText(content, "text/html");
    mailSender.send(mimeMessage);
  }

  private boolean isDefaultRow(List<String> rowData) {
    return rowData.get(1).equals(BookmarkName.FULL_NAME) || rowData.get(2).equals(BookmarkName.CURRENT_TITLE);
  }

  private String removeAdvertise(String htmlContent) {
    return htmlContent.replace(AdvertiseString.SPIRE_DOC_AD, "");
  }


  private void convertImageToBase64(String filePath) throws IOException {
    File file = new File(filePath);
    org.jsoup.nodes.Document document = Jsoup.parse(file, "UTF-8");


    //get all images by getting the img card, image of document change so the document will change too
    Elements images = document.getElementsByTag("img");
    for (Element image : images) {
      File imageFile = new File("html/" + image.attr("src"));
      //because in img card, the src storing the image path and then parse it to byte
      byte[] imgFileContent = Files.readAllBytes(imageFile.toPath());
      
      //encode all the byte of the image
      String base64Img = Base64.getEncoder().encodeToString(imgFileContent);
      log.info("====> base64 image: {}", base64Img);
      String imgStyle = imageFile.getName().substring(imageFile.getName().indexOf('.') + 1);
      log.info("====> img style: {}", imgStyle);
      image.attr("src", "data:image/" + imgStyle + ";base64," + base64Img);
    }
    //save to new file
    Files.writeString(Paths.get("html/template-base64-img.html"), document.outerHtml());
  }

  private String getHtmlContent(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

}
