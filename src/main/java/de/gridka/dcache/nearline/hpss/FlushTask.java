package de.gridka.dcache.nearline.hpss;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import org.dcache.pool.nearline.spi.FlushRequest;
import org.dcache.vehicles.FileAttributes;

import diskCacheV111.util.CacheException;

class FlushTask implements Callable<Set<URI>> {
  private String type;
  private String name;
  private Path path;
  private Path externalPath;
  private String hsmPath;
  
  public FlushTask(String type, String name, FlushRequest request, String mountpoint) {
    this.type = type;
    this.name = name;
    
    FileAttributes fileAttributes = request.getFileAttributes();
    String pnfsId = fileAttributes.getPnfsId().toString();
    this.path = request.getFile().toPath();
    
    StringBuilder sb = new StringBuilder();
    sb.append('/' + fileAttributes.getStorageInfo().getKey("store"));
    sb.append('/' + pnfsId.substring(0, 5));
    sb.append('/' + pnfsId.charAt(5));
    sb.append('/' + pnfsId);
    this.hsmPath = sb.toString();
    this.externalPath = Paths.get(mountpoint, hsmPath);
  }
  
  public Set<URI> call () throws CacheException, URISyntaxException {
    try {
      Files.copy(path, externalPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new CacheException(2, "Copy to " + externalPath.toString() + " failed.", e);
    }
    
    URI uri = new URI(type, name, hsmPath, null, null);
    return Collections.singleton(uri);
  }
}