package org.eurekaclinical.i2b2.client;

/*
 * #%L
 * i2b2 Export Service
 * %%
 * Copyright (C) 2013 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import org.eurekaclinical.i2b2.client.comm.I2b2AuthMetadata;
import org.eurekaclinical.i2b2.client.comm.I2b2PatientSet;
import org.eurekaclinical.i2b2.client.comm.I2b2Concept;
import org.eurekaclinical.i2b2.client.props.I2b2Properties;
import org.eurekaclinical.i2b2.client.pdo.I2b2PdoResultParser;
import org.eurekaclinical.i2b2.client.pdo.I2b2PdoResults;
import org.eurekaclinical.i2b2.client.xml.I2b2XmlException;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

/**
 * Implementation of the i2b2 PDO retriever interface. It retrieves the data by
 * filling the PDO request XML template and sending that XML to the i2b2 service
 * as defined the application's properties file (see {@link I2b2CommUtil}).
 *
 * @author Michel Mansour
 * @since 1.0
 */
public final class I2b2PdoRetrieverImpl implements I2b2PdoRetriever {

    private final Configuration config;
    private final I2b2XmlPostSupport i2b2XmlPostSupport;
    private final I2b2Properties properties;

    /**
     * Default no-arg constructor.
     * @param inProperties
     * @param inI2b2XmlPostSupport
     */
    @Inject
    public I2b2PdoRetrieverImpl(I2b2Properties inProperties,
            I2b2XmlPostSupport inI2b2XmlPostSupport) {
        this.config = new Configuration();
        this.config.setClassForTemplateLoading(this.getClass(), "/");
        this.config.setObjectWrapper(new DefaultObjectWrapper());
        this.config.setNumberFormat("0.######");  // to prevent addition of commas to numbers
        // FreeMarker uses the locale to format numbers
        // in a human-readable way, but this XML is not
        // for humans.
        this.i2b2XmlPostSupport = inI2b2XmlPostSupport;
        this.properties = inProperties;
    }

    @Override
    public I2b2PdoResults retrieve(I2b2AuthMetadata authMetadata,
            Collection<I2b2Concept> concepts,
            I2b2PatientSet patientSet) throws I2b2XmlException {
        try {
            Template tmpl = this.config.getTemplate(I2b2CommUtil.TEMPLATES_DIR + "/i2b2_pdo_request.ftl");
            StringWriter writer = new StringWriter();

            String messageId = this.i2b2XmlPostSupport.generateMessageId();

            Map<String, Object> params = new HashMap<>();
            params.put("redirectHost", this.properties.getI2b2ServiceHostUrl());
            params.put("domain", authMetadata.getDomain());
            params.put("username", authMetadata.getUsername());
            params.put("passwordNode", authMetadata.getPasswordNode());
            params.put("messageId", messageId);
            params.put("projectId", authMetadata.getProjectId());
            params.put("patientListMax", patientSet.getPatientSetSize());
            params.put("patientListMin", "1");
            params.put("patientSetCollId", patientSet.getPatientSetCollId());
            params.put("items", concepts);

            tmpl.process(params, writer);
            Document respXml = this.i2b2XmlPostSupport.postXmlToI2b2(writer.toString());
            I2b2PdoResultParser parser = new I2b2PdoResultParser(respXml);
            return parser.parse();
        } catch (IOException | TemplateException | SAXException | ParserConfigurationException e) {
            throw new I2b2XmlException(e);
        }
    }
}