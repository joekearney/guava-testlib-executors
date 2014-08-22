package com.google.common.util.concurrent;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PackagePrivateAccessorForGuava {
    private PackagePrivateAccessorForGuava() {}
    
    public static SerializingExecutor newSerializingExecutor(Executor executor) {
    	/*
    	 * disable logger - by default it logs at SEVERE exceptions that were thrown by tasks
    	 */
        Logger.getLogger(SerializingExecutor.class.getName()).setLevel(Level.OFF);

        return new SerializingExecutor(executor);
    }
}
