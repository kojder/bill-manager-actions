package com.example.bill_manager.upload;

import java.util.List;

public interface PdfConversionService {

  List<byte[]> convertToImages(byte[] pdfContent);
}
