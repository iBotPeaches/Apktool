When assessing art changes for impact to baksmali/dexlib2's deodexing functionality, there are a
few key structures to keep an eye on.

- The oat version is stored in runtime/oat.h
- The OatHeader structure in runtime/oat.h
- The OatDexFile structure, as it is written in OatWriter::OatDexFile::Write in
  compiler/oat_writer.cc (later moved to dex2oat/linker/oat_writer.cc)