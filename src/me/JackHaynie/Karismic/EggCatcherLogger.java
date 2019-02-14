package me.JackHaynie.Karismic;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class EggCatcherLogger
{
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  private File file;
  
  public EggCatcherLogger(File file)
  {
    this.file = file;
  }
  
  public void logToFile(String message)
  {
    if (!this.file.exists()) {
      try
      {
        this.file.createNewFile();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    PrintWriter pw = null;
    try
    {
      pw = new PrintWriter(new FileWriter(this.file, true));
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    pw.println(this.dateFormat.format(Long.valueOf(System.currentTimeMillis())) + " " + message);
    pw.flush();
    pw.close();
  }
  
  public File getFile()
  {
    return this.file;
  }
  
  public void setFile(File file)
  {
    this.file = file;
  }
}
