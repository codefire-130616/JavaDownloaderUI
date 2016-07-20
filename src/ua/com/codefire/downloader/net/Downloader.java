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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author CodeFireUA <edu@codefire.com.ua>
 */
public class Downloader implements Runnable {

    private final File store;
    private List<DownloaderTask> downloaderTasks;

    private List<DownloaderListener> listeners;
    private ExecutorService threadPool;

    public Downloader(File store) {
        this.store = store;
        this.listeners = Collections.synchronizedList(new ArrayList<DownloaderListener>());
    }

    public List<DownloaderTask> getDownloaderTasks() {
        return downloaderTasks;
    }

    public void setDownloaderTasks(List<DownloaderTask> downloaderTasks) {
        this.downloaderTasks = downloaderTasks;
    }

    public boolean add(DownloaderListener listener) {
        return listeners.add(listener);
    }

    public boolean remove(DownloaderListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public void run() {
        // USE TRHEAD POOL
        threadPool = Executors.newFixedThreadPool(3);
        
        for (DownloaderTask task : downloaderTasks) {
            threadPool.execute(task);
        }

        threadPool.shutdown();
    }

    public void retrieveFiles(URL fileList) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DownloaderTask> tasks = new ArrayList<>();

                try (Scanner scanner = new Scanner(fileList.openStream())) {
                    while (scanner.hasNextLine()) {
                        DownloaderTask downloaderTask = new DownloaderTask(Downloader.this, store, new URL(scanner.nextLine()));
                        downloaderTask.prepare();

                        tasks.add(downloaderTask);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(Downloader.class.getName()).log(Level.SEVERE, null, ex);
                }

                for (DownloaderListener listener : listeners) {
                    listener.downloadPrepared(tasks);
                }
            }
        }).start();
    }

    public void downloadBegin(DownloaderTask task) {
        for (DownloaderListener listener : listeners) {
            listener.downloadBegin(task);
        }
    }

    public void downloadProgress(DownloaderTask task) {
        for (DownloaderListener listener : listeners) {
            listener.downloadProgress(task);
        }

    }

    public void downloadComplete(DownloaderTask task) {
        for (DownloaderListener listener : listeners) {
            listener.downloadComplete(task);
        }
    }

    public void download(List<DownloaderTask> downloaderTasks) {
        this.downloaderTasks = downloaderTasks;
        
        new Thread(this).start();
    }
}
