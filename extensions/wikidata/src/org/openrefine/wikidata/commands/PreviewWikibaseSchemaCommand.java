/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.wikidata.commands;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.commands.Command;

import org.openrefine.wikidata.exporters.QuickStatementsExporter;
import org.openrefine.wikidata.schema.WikibaseSchema;
import com.google.refine.model.Project;
import com.google.refine.util.ParsingUtilities;

public class PreviewWikibaseSchemaCommand extends Command {
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            String jsonString = request.getParameter("schema");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            WikibaseSchema schema = WikibaseSchema.reconstruct(json);
            
            StringWriter sb = new StringWriter(2048);
            JSONWriter writer = new JSONWriter(sb, 32);
            writer.object();
            
            {
                StringWriter stringWriter = new StringWriter();
                QuickStatementsExporter exporter = new QuickStatementsExporter();
                exporter.translateSchema(project, schema, stringWriter);
                
                String fullQS = stringWriter.toString();
                stringWriter = new StringWriter();
                LineNumberReader reader = new LineNumberReader(new StringReader(fullQS));
                reader.setLineNumber(0);
                int maxQSLinesForPreview = 50;
                for(int i = 0; i != maxQSLinesForPreview; i++) {
                    stringWriter.write(reader.readLine()+"\n");
                }
                if (reader.getLineNumber() == maxQSLinesForPreview) {
                    stringWriter.write("...");
                }
                
                writer.key("quickstatements");
                writer.value(stringWriter.toString());
            }
            
            writer.endObject();
            
            respond(response, sb.toString());
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}
