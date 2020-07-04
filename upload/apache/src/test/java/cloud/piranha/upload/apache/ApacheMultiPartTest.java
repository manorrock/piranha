/*
 * Copyright (c) 2002-2020 Manorrock.com. All Rights Reserved.
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
package cloud.piranha.upload.apache;

import java.io.File;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The JUnit tests for the ApacheMultiPart class.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 */
public class ApacheMultiPartTest {

    /**
     * Test delete method.
     *
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testDelete() throws Exception {
        File file = new File("delete.me");
        file.createNewFile();
        assertTrue(file.exists());
        FileItem item = new DiskFileItem("fieldName", "text/html", false,
                "delete.me", 0, file);
        ApacheMultiPart part = new ApacheMultiPart(item);
        part.delete();
        file.delete();
    }

    /**
     * Test getContentType method.
     * 
     * @throws Exception when a serious error occurs.
     */
    @Test
    public void testGetContentType() throws Exception {
        File file = new File("delete.me");
        file.createNewFile();
        assertTrue(file.exists());
        FileItem item = new DiskFileItem("fieldName", "text/html", false,
                "delete.me", 0, file);
        ApacheMultiPart part = new ApacheMultiPart(item);
        assertEquals("text/html", part.getContentType());
        file.delete();
    }
}
