package org.seachordsmen.ttrack.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Worker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Worker.class);
    private Player player;

    public Worker(Player player) {
        this.player = player;
    }
    
    protected Player getPlayer() { return player; }
    
    public void start() {
        LOG.debug("{}: starting", getClass().getSimpleName());
        schedule();
    }
    
    public void run() {
        if (player.isPlaying()) {
            //LOG.debug("{}: processing", getClass().getSimpleName());
            try {
                player.getComponentReadLock().lock();
                if (player.isPlaying()) {
                    process();
                }
            } finally {
                player.getComponentReadLock().unlock();
            }
        } else {
            //LOG.debug("{}: sleeping", getClass().getSimpleName());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Player.LOG.warn("Sleep interrupted", e);
            }
        }
        schedule();
    }

    private void schedule() {
        player.getHandler().post(this);
    }
    
    protected abstract void process();
}