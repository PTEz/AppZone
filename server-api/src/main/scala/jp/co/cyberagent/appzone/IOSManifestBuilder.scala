/*
 * Copyright (C) 2013 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.cyberagent.appzone

import org.scalatra.servlet.FileItem
import java.io.InputStream
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import com.dd.plist.PropertyListParser
import com.dd.plist.NSDictionary
import java.io.ByteArrayInputStream

class IOSManifestBuilder(ipaFile: FileItem) {
  def createManifest(url: String): InputStream = {
    val tempFile = File.createTempFile("asdf", "")
    ipaFile.write(tempFile)
    val zip = new ZipFile(tempFile)
    val entries = zip.entries()

    var entry: ZipEntry = null
    while (entries.hasMoreElements && entry == null) {
      val element = entries.nextElement()
      if (element.getName.toLowerCase.endsWith("/info.plist")) {
        entry = element
      }
    }
    val plistInputStream = zip.getInputStream(entry)

    val rootDict = PropertyListParser.parse(plistInputStream).asInstanceOf[NSDictionary]
    val manifest = IOSManifestBuilder.getManifestTemplate
      .replace("${url}", url)
      .replace("${CFBundleName}", rootDict.objectForKey("CFBundleName").toString)
      .replace("${CFBundleIdentifier}", rootDict.objectForKey("CFBundleIdentifier").toString)
      .replace("${CFBundleShortVersionString}", rootDict.objectForKey("CFBundleShortVersionString").toString + " (" + rootDict.objectForKey("CFBundleVersion").toString + ")")

    new ByteArrayInputStream(manifest.getBytes)
  }
}

object IOSManifestBuilder {
  val IOS_MANIFEST_TEMPLATE = new File(getClass.getResource("/OTAManifestTemplate.plist").toURI())
  def getManifestTemplate: String = {
    scala.io.Source.fromFile(IOS_MANIFEST_TEMPLATE).mkString
  }
}