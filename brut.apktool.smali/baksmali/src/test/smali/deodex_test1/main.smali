.class public Lmain;

.super Ljava/lang/Object;

.method public static main([Ljava/lang/String;)V
    .registers 3

    :here4
    const v0, 0

    :here3

    new-instance v2, Lsuperclass;
    invoke-direct {v2}, Lsuperclass;-><init>()V

    if-eqz v0, :here


    #this is the unresolvable instruction. v0 is always null,
    #and this will always throw an exception. It should be
    #replaced with throw v0.
    invoke-virtual {v0}, Lrandomclass;->getSuperclass()Lsuperclass;
    move-result-object v1


    if-eqz v0, :here

    #this would normally be deodexable, except that it follows
    #the above un-deodexeable instruction, which prevents the
    #propagation of any register information. It can never be
    #reached, and should be replaced with throw v2
    invoke-virtual {v2}, Lsuperclass;->somemethod()V

    #another odexed instruction that uses the result of the
    #first unresolveable odex instruction. This should
    #be replaced with throw v1
    invoke-virtual {v1}, Lsuperclass;->somemethod()V

    :here

    #and we're back to the non-dead code
    invoke-virtual {v2}, Lsuperclass;->somemethod()V

    if-nez v0, :here3

    return-void
.end method

.method public static FirstInstructionTest(Lrandomclass;)V
    .registers 1

    :try_start
        invoke-virtual/range {p0}, Lrandomclass;->getSuperclass()Lsuperclass;
        return-void
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :handler
    :handler
        :inner_try_start
            #this tests that the parameter register types are correctly propagated to the exception handlers, in the
            #case that the first instruction of the method can throw an exception and is in a try black
            invoke-virtual/range {p0}, Lrandomclass;->getSuperclass()Lsuperclass;
            return-void
        :inner_try_end
        .catch Ljava/lang/Exception; {:inner_try_start .. :inner_try_end} :inner_handler
        :inner_handler
            #this additionally tests that the register types are propagated recursively, in the case that the first
            #instruction in the exception handler can also throw an exception, and is covered by a try block
            invoke-virtual/range {p0}, Lrandomclass;->getSuperclass()Lsuperclass;
            return-void
.end method