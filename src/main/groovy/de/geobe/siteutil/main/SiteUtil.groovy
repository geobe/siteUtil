/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020.  Georg Beier. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.geobe.siteutil.main

import de.geobe.siteutil.config.RecentValue
import de.geobe.siteutil.dns.IspDnsConfig
import de.geobe.siteutil.dns.MyIp
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by georg beier on 04.05.2020.
 */
class SiteUtil {

    private static CONFIGFILE = 'liveConfig.cfg'

    private static ACME_URI = 'acme://letsencrypt.org/staging'

    private static final Logger LOG = LoggerFactory.getLogger(SiteUtil.class)

    static ToDo = '''
-done- test _acme entry config in DNS
write demo workflow method: 
   -done-  get dynIP, 
   -done-  set IP DNS entry, 
    get DNS challenge from LetsEncrypt, 
    set DNS challenge,
    get and save certificate and key from LetsEncrypt,
    clear DNS challenge
test demo workflow on raspi
configure https access to raspi
test certificate with Go program on raspi
write certificate integration Groovy <-> Go
-done- write bookkeeping class for recent changes of IP and certificate
write real time program to automate IP and certificate update
test coexistence of siteutil, reverseproxy and hotpuma on a single raspi
distribute to updated sd and install in hotpuma hardware
'''

    RecentValue recentValue = RecentValue.value
    IspDnsConfig dnsConfig = new IspDnsConfig(CONFIGFILE)

    def maintainIP() {
        def dynIp = new MyIp().myIp
        if (recentValue.lastIp != dynIp) {
            def ret = dnsConfig.ipAddress = dynIp
            recentValue.lastIp = dynIp
            recentValue.ipSetAt = new Date()
            recentValue.save()
            ret
        } else {
            'nothing to do'
        }
    }

    public static void main(String[] args) {
        println ToDo
        de.geobe.siteutil.main.SiteUtil siteUtil = new SiteUtil()
//        siteUtil.dnsConfig.waitOnPage = true
        def out = siteUtil.maintainIP()
        LOG.info("maintainIp returned $out")
    }

}

