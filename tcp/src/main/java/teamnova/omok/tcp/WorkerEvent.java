package teamnova.omok.tcp;

/**
 * Logical event dispatched to worker threads.
 */
interface WorkerEvent {
    void execute(NioReactorServer server);
}
