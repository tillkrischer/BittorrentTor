package config;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager {

  private String filename;

  public ConfigManager(String filename) {
    this.filename = filename;
  }

  public Config getConfig() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Config conf = null;
    try {
      FileReader fr = new FileReader(filename);
      conf = gson.fromJson(fr, Config.class);
    } catch (FileNotFoundException e) {
      try {
        conf = new Config();
        FileWriter fw = new FileWriter(filename);
        fw.write(gson.toJson(conf));
        System.out.println(gson.toJson(conf));
        fw.close();
      } catch (IOException e1) {
        System.out.println("error writing config file");
      }
    }
    return conf;
  }
}
