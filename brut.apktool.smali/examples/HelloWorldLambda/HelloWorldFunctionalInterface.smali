# Functional interface used by HelloWorld.smali
# Required in order to reify the lambda with create-lambda or unbox-lambda instructions

.class public abstract interface LHelloWorldFunctionalInterface;
.super Ljava/lang/Object;

.method public abstract applyFourStrings(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
.end method
