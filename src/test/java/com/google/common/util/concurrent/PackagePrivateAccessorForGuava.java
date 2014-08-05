package com.google.common.util.concurrent;

import java.util.concurrent.Executor;

public class PackagePrivateAccessorForGuava {
    public static SerializingExecutor newSerializingExecutor(Executor executor) {
        return new SerializingExecutor(executor);
    }
}