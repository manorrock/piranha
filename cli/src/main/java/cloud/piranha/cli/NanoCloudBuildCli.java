/*
 * Copyright (c) 2002-2021 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice, 
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its 
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.cli;

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * The Piranha Nano Cloud Build CLI.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
@CommandLine.Command(name = "build", description = "Build Piranha Nano application for the Cloud")
public class NanoCloudBuildCli implements Runnable {

    /**
     * Execute 'pi nano cloud build'.
     */
    @Override
    public void run() {
        try {
            System.out.println("Executing - docker build --target zip -t zip -f Dockerfile .");
            ProcessBuilder builder = new ProcessBuilder();
            Process process = builder.inheritIO().command("docker build --target zip -t zip -f Dockerfile .".split(" ")).start();
            process.waitFor(1, HOURS);

            System.out.println("Executing - docker rm zip");
            builder = new ProcessBuilder();
            process = builder.inheritIO().command("docker rm zip".split(" ")).start();
            process.waitFor(1, HOURS);

            System.out.println("Executing - docker create --name zip zip");
            builder = new ProcessBuilder();
            process = builder.inheritIO().command("docker create --name zip zip".split(" ")).start();
            process.waitFor(1, HOURS);
            
            File directory = new File("target");
            if (!directory.exists()) {
                System.out.println("Executing - creating target directory");
                directory.mkdirs();
            }
            
            System.out.println("Executing - docker cp zip:/usr/local/azure.zip target/.");
            builder = new ProcessBuilder();
            process = builder.inheritIO().command("docker cp zip:/usr/local/azure.zip target/.".split(" ")).start();
            process.waitFor(1, HOURS);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
        }
    }
}
