package org.xm.xmnlp.corpus.document;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xm.xmnlp.corpus.document.word.IWord;
import org.xm.xmnlp.corpus.document.word.Word;
import org.xm.xmnlp.util.FolderWalker;
import org.xm.xmnlp.util.IOUtil;

import java.io.File;
import java.util.LinkedList;
import java.util.List;


/**
 * 加载
 */
public class CorpusLoader {
    private static Logger logger = LogManager.getLogger();

    public static void walk(String folderPath, Handler handler) {
        long start = System.currentTimeMillis();
        List<File> fileList = FolderWalker.open(folderPath);
        int i = 0;
        for (File file : fileList) {
            System.out.print(file);
            Document document = convert2Document(file);
            System.out.println(" " + ++i + " / " + fileList.size());
            handler.handle(document);
        }
        System.out.printf("花费时间%d ms\n", System.currentTimeMillis() - start);
    }

    public static void walk(String folderPath, HandlerThread[] threadArray) {
        long start = System.currentTimeMillis();
        List<File> fileList = FolderWalker.open(folderPath);
        for (int i = 0; i < threadArray.length - 1; ++i) {
            threadArray[i].fileList = fileList.subList(fileList.size() / threadArray.length * i, fileList.size() / threadArray.length * (i + 1));
            threadArray[i].start();
        }
        threadArray[threadArray.length - 1].fileList = fileList.subList(fileList.size() / threadArray.length * (threadArray.length - 1), fileList.size());
        threadArray[threadArray.length - 1].start();
        for (HandlerThread handlerThread : threadArray) {
            try {
                handlerThread.join();
            } catch (InterruptedException e) {
                logger.warn("多线程异常" + e);
            }
        }
        System.out.printf("花费时间%d ms\n", System.currentTimeMillis() - start);
    }

    public static List<Document> convert2DocumentList(String folderPath) {
        long start = System.currentTimeMillis();
        List<File> fileList = FolderWalker.open(folderPath);
        List<Document> documentList = new LinkedList<Document>();
        int i = 0;
        for (File file : fileList) {
            System.out.print(file);
            Document document = convert2Document(file);
            documentList.add(document);
            System.out.println(" " + ++i + " / " + fileList.size());
        }
        System.out.println(documentList.size());
        System.out.printf("花费时间%d ms\n", System.currentTimeMillis() - start);
        return documentList;
    }

    public static List<Document> loadCorpus(String path) {
        return (List<Document>) IOUtil.readObjectFrom(path);
    }

    public static boolean saveCorpus(List<Document> documentList, String path) {
        return IOUtil.saveObjectTo(documentList, path);
    }

    public static List<List<IWord>> loadSentenceList(String path) {
        return (List<List<IWord>>) IOUtil.readObjectFrom(path);
    }

    public static boolean saveSentenceList(List<List<IWord>> sentenceList, String path) {
        return IOUtil.saveObjectTo(sentenceList, path);
    }

    public static List<List<IWord>> convert2SentenceList(String path) {
        List<Document> documentList = CorpusLoader.convert2DocumentList(path);
        List<List<IWord>> simpleList = new LinkedList<List<IWord>>();
        for (Document document : documentList) {
            for (Sentence sentence : document.sentenceList) {
                simpleList.add(sentence.wordList);
            }
        }

        return simpleList;
    }

    public static List<List<Word>> convert2SimpleSentenceList(String path) {
        List<Document> documentList = CorpusLoader.convert2DocumentList(path);
        List<List<Word>> simpleList = new LinkedList<List<Word>>();
        for (Document document : documentList) {
            simpleList.addAll(document.getSimpleSentenceList());
        }

        return simpleList;
    }

    public static Document convert2Document(File file) {
        Document document = Document.create(IOUtil.readTxt(file.getPath()));
        if (document != null) {
            return document;
        } else {
            System.exit(-1);
        }
        return null;
    }

    public interface Handler {
        void handle(Document document);
    }

    /**
     * 多线程任务
     */
    public static abstract class HandlerThread extends Thread implements Handler {
        /**
         * 这个线程负责处理这些事情
         */
        public List<File> fileList;

        public HandlerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            System.out.printf("线程#%s 开始运行\n", getName());
            int i = 0;
            for (File file : fileList) {
                System.out.print(file);
                Document document = convert2Document(file);
                System.out.println(" " + ++i + " / " + fileList.size());
                handle(document);
            }
            System.out.printf("线程#%s 运行完毕，耗时%dms\n", getName(), System.currentTimeMillis() - start);
        }
    }
}
