.class public LHelloWorldLambda;

#Ye olde hello world application (with lambdas!)
#To assemble and run this on a phone or emulator:
#
#java -jar smali.jar -o classes.dex HelloWorldLambda.smali HelloWorldFunctionalInterface.smali
#zip HelloWorld.zip classes.dex
#adb push HelloWorld.zip /data/local
#adb shell dalvikvm -cp /data/local/HelloWorld.zip HelloWorld
#
#if you get out of memory type errors when running smali.jar, try
#java -Xmx512m -jar smali.jar HelloWorldLambda.smali
#instead

.super Ljava/lang/Object;

.method public static doHelloWorld(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    .registers 6 # 4 parameters, 2 locals
    liberate-variable v0, p0, "helloworld"

    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;
    invoke-virtual {v1, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    return-void
.end method

.method public static main([Ljava/lang/String;)V
    .registers 9 # 1 parameter, 8 locals

    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-string v1, "Hello World!"
    const-string v2, "How" # vD
    const-string v3, "are" # vE
    const-string v4, "you" # vF
    const-string v5, "doing?" # vG

    capture-variable v1, "helloworld"

    # TODO: do I need to pass the type of the lambda's functional interface here as a type id?
    create-lambda v1, LHelloWorldLambda;->doHelloWorld(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    # Method descriptor is not required here, because only the single-abstract method is ever invoked.
    invoke-lambda v1, {v2, v3, v4, v5}

    box-lambda v6, v1
    invoke-virtual {v6, v2, v3, v4, v5}, LHelloWorldFunctionalInterface;->applyFourStrings(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V

    # FIXME: should be \HelloWorldFunctionalInterface; instead of L...;

    # TODO: do we really need the type descriptor here at all?
    unbox-lambda v7, v6, LHelloWorldFunctionalInterface;
    invoke-lambda v7, {v2, v3, v4, v5}

    return-void
.end method
