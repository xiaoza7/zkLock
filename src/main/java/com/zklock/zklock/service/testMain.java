package com.zklock.zklock.service;

import java.util.concurrent.locks.Lock;

public class testMain {
    public static void main(String[] args) {
        Thread t1 = new Thread(new ClientThread(),"client1");
        Thread t2 = new Thread(new ClientThread(),"client2");
        t1.start();
        t2.start();
    }

    static Lock lock =new ZkLock();

    static class ClientThread implements Runnable{


        @Override
        public void run() {


            lock.lock();
            System.out.println("begin the transaction.........");
            lock.unlock();
        }
    }
}
