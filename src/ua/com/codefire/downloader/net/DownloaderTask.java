/*
 * Copyright (C) 2016 CodeFireUA <edu@codefire.com.ua>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ua.com.codefire.downloader.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CodeFireUA <edu@codefire.com.ua>
 */
public class DownloaderTask implements Runnable {

    private final Downloader downloader;
    private final File store;
    private URL source;
    private String sourceAddress;
    private File target;
    private int bufferSize = 16384;
    private long downloaded;
    private long total;
    private URLConnection conn;
    private boolean downloading;

    public DownloaderTask(Downloader downloader, File store, URL source) {
        this.downloader = downloader;
        this.store = store;
        this.source = source;
    }

    public URL getSource() {
        return source;
    }

    public File getTarget() {
        return target;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void prepare() {
        try {
            conn = source.openConnection();
            total = conn.getContentLengthLong();

            source = conn.getURL();

            sourceAddress = URLDecoder.decode(new String(source.toString().getBytes("ISO-8859-1"), "UTF-8"), "UTF-8");
            String sourcefile = URLDecoder.decode(new String(source.getFile().getBytes("ISO-8859-1"), "UTF-8"), "UTF-8");

            target = new File(store, new File(sourcefile).getName());
        } catch (IOException ex) {
            Logger.getLogger(DownloaderTask.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        downloading = true;
        downloader.downloadBegin(this);

        try (BufferedInputStream bis = new BufferedInputStream(source.openStream());
                FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[bufferSize];

            for (int read; (read = bis.read(buffer)) >= 0;) {
                fos.write(buffer, 0, read);
                fos.flush();

                downloaded += read;

                if (read > 0) {
                    downloader.downloadProgress(this);
                }
            }
            System.out.println();
        } catch (IOException ex) {
            Logger.getLogger(DownloaderTask.class.getName()).log(Level.SEVERE, null, ex);
        }

        downloader.downloadComplete(this);
    }

    @Override
    public String toString() {
        if (downloading) {
            return String.format("%s\n[%5.2f%%]", target.getName(), (double)downloaded * 100 / total);
        } else {
            return String.format("%s\n[%dbytes]", target.getName(), total);
        }
    }

}
