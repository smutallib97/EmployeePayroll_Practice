package com.bridgelabz;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class Java8WatchServiceExample {

    private final WatchService watcher;
    private final Map<WatchKey, Path> dirWatchers;

    public Java8WatchServiceExample(Path dir) throws IOException {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.dirWatchers = new HashMap<WatchKey,Path>();
        scanAndRegisterWatchers(dir);
    }

    private void registerWatchers(Path dir) throws IOException{
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_DELETE,StandardWatchEventKinds.ENTRY_MODIFY);
        dirWatchers.put(key, dir);
    }

    private void scanAndRegisterWatchers(final Path start) throws IOException{
        Files.walkFileTree(start, new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException{
                registerWatchers(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public void processEvents() {

        while (true) {
            WatchKey key;
            try {
                key = watcher.take();
            }
            catch(InterruptedException e) {return;}

            Path dir = dirWatchers.get(key);
            if (dir==null) continue;
            for(WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                Path name = ((WatchEvent<Path>)event).context();
                Path child = dir.resolve(name);
                System.out.format("%s: %s\n", event.kind().name(),child);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child)) scanAndRegisterWatchers(child);
                    }catch(IOException e) {}
                }else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    if (Files.isDirectory(child)) dirWatchers.remove(key);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                dirWatchers.remove(key);
                if (dirWatchers.isEmpty())break;
            }
        }


    }
}
