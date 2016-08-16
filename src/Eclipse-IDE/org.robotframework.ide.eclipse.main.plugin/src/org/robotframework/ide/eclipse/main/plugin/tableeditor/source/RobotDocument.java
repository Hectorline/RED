package org.robotframework.ide.eclipse.main.plugin.tableeditor.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.rf.ide.core.testdata.RobotParser;
import org.rf.ide.core.testdata.RobotParser.RobotParserConfig;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.RobotProjectHolder;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

public class RobotDocument extends Document {

    private static final int DELAY = 250;
    private static final int LIMIT = 800;

    private boolean hasNewestVersion = false;
    private final Semaphore parsingSemaphore = new Semaphore(1);
    private final Semaphore parsingFinishedSemaphore = new Semaphore(1);
    private boolean reparseInSameThread = true;

    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private Runnable parsingRunnable;

    private final Supplier<RobotSuiteFile> fileModelSupplier;
    private RobotParser parser;
    private File file;

    private RobotFileOutput output;

    private final List<IRobotDocumentParsingListener> parseListeners = new ArrayList<>();

    public RobotDocument(final Supplier<RobotSuiteFile> fileModelSupplier) {
        this.fileModelSupplier = fileModelSupplier;
    }

    @VisibleForTesting
    public RobotDocument(final RobotParser parser, final File file) {
        this.fileModelSupplier = new Supplier<RobotSuiteFile>() {
            @Override
            public RobotSuiteFile get() {
                return null;
            }
        };
        this.parser = parser;
        this.file = file;
    }

    public void addParseListener(final IRobotDocumentParsingListener listener) {
        parseListeners.add(listener);
    }

    public void removeParseListener(final IRobotDocumentParsingListener listener) {
        parseListeners.remove(listener);
    }

    public boolean hasNewestModel() {
        return hasNewestVersion;
    }

    @Override
    protected void fireDocumentAboutToBeChanged(final DocumentEvent event) {
        createParserIfNeeded();
        reparseInSameThread = getNumberOfLines() < LIMIT;
        if (!reparseInSameThread) {
            try {
                parsingSemaphore.acquire();
            } catch (final InterruptedException e) {
                throw new IllegalStateException("Document reparsing interrupted!", e);
            }
        }
        hasNewestVersion = false;
        super.fireDocumentAboutToBeChanged(event);
    }

    private void createParserIfNeeded() {
        if (parser == null) {
            parser = createParser(fileModelSupplier.get());
            file = new File(fileModelSupplier.get().getName());
        }
    }

    @Override
    protected void fireDocumentChanged(final DocumentEvent event) {
        if (reparseInSameThread) {
            // short documents can be reparsed in the same thread as this does not
            // affect performance too much
            reparse();
        } else {
            reparseInSeparateThread();
        }
        super.fireDocumentChanged(event);
    }

    private void reparse() {
        output = parser.parseEditorContent(get(), file);
        for (final IRobotDocumentParsingListener listener : parseListeners) {
            listener.reparsingFinished(output);
        }
        hasNewestVersion = true;
    }

    private void reparseInSeparateThread() {
        if (parsingRunnable != null) {
            executor.remove(parsingRunnable);
        }
        parsingRunnable = new Runnable() {
            @Override
            public void run() {
                reparse();
                parsingSemaphore.release();
            }
        };
        executor.schedule(parsingRunnable, DELAY, TimeUnit.MILLISECONDS);
    }

    private Future<RobotFileOutput> getNewestOutput() {
        return new Future<RobotFileOutput>() {

            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                // not supported
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return hasNewestVersion;
            }

            @Override
            public RobotFileOutput get() throws InterruptedException, ExecutionException {
                // we don't want situation, when two threads acquire parsingSemaphore and parsing
                // task cannot be performed
                parsingFinishedSemaphore.acquire();
                parsingSemaphore.acquire();
                try {
                    return output;
                } finally {
                    parsingSemaphore.release();
                    parsingFinishedSemaphore.release();
                }
            }

            @Override
            public RobotFileOutput get(final long timeout, final TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
                throw new IllegalStateException("Operation not supported");
            }
        };
    }

    /**
     * Gets newest parsed model. Waits for reparsing end if needed. IllegalStateException is thrown
     * when waiting has been interrupted.
     * 
     * @return
     */
    public RobotFile getNewestModel() {
        return getNewestFileOutput().getFileModel();
    }

    /**
     * Gets newest parsed file output. Waits for reparsing end if needed. IllegalStateException is
     * thrown when waiting has been interrupted.
     * 
     * @return
     */
    public RobotFileOutput getNewestFileOutput() {
        try {
            return getNewestOutput().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IllegalStateException("Waiting for newest model has been interrupted", e);
        }
    }

    private static RobotParser createParser(final RobotSuiteFile model) {
        final RobotParserConfig parserCfg = new RobotParserConfig();
        parserCfg.setEagerImport(false);
        parserCfg.setIncludeImportVariables(false);

        final RobotProjectHolder holder = model.getFile() == null ? new RobotProjectHolder()
                : model.getProject().getRobotProjectHolder();
        return RobotParser.create(holder, parserCfg);
    }
    
    public static interface IRobotDocumentParsingListener {

        void reparsingFinished(RobotFileOutput parsedOutput);
    }
}