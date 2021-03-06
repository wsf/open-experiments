/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.ldap;

import static org.easymock.EasyMock.isA;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapException;

import java.net.URL;

/**
 *
 */
public class SimpleLdapConnectionManagerTest {
  private LdapConnectionManagerConfig config;
  private SimpleLdapConnectionManager mgr;
  private LDAPConnection conn;
  private String keystoreLocation;
  private String keystorePassword = "keystore123";

  @Before
  public void setUp() {
    URL url = getClass().getResource("server_keystore.jks");
    keystoreLocation = url.getPath();

    config = new LdapConnectionManagerConfig();
    config.setLdapHost("localhost");
    config.setLdapPort(LDAPConnection.DEFAULT_PORT);
    config.setLdapUser("ldapUser");
    config.setLdapPassword("ldapPassword");

    conn = createMock(LDAPConnection.class);

    mgr = new SimpleLdapConnectionManager() {
      @Override
      protected LDAPConnection newLDAPConnection() {
        return conn;
      }
    };

    mgr.setConfig(config);
  }

  @After
  public void tearDown() {
    mgr.destroy();
  }

  @Test
  public void testGetConfig() {
    assertEquals(mgr.getConfig(), config);
  }

  @Test
  public void testNewLdapConnection() {
    new SimpleLdapConnectionManager().newLDAPConnection();
  }

  @Test
  public void testConnection() throws Exception {
    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    replay(conn);

    mgr.init();
    mgr.getConnection();
  }

  @Test(expected = LdapException.class)
  public void testConnectionCantConnect() throws Exception {
    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());
    expectLastCall().andThrow(new LDAPException());
    replay(conn);

    mgr.init();
    mgr.getConnection();
    fail("Should throw an exception when can't connect.");
  }

  @Test(expected = LdapException.class)
  public void testConnectionLdapFailPostConnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new LDAPException());

    conn.disconnect();

    replay(conn);
    mgr.getConnection();
    fail("Should throw an exception when can't start TLS.");
  }

  @Test(expected = LdapException.class)
  public void testConnectionLdapFailPostConnectFailDisconnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new LDAPException());

    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());

    replay(conn);
    mgr.getConnection();
    fail("Should throw an exception when can't start TLS.");
  }

  @Test(expected = RuntimeException.class)
  public void testConnectionRuntimeFailPostConnect() throws LDAPException, LdapException {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new RuntimeException());

    conn.disconnect();

    replay(conn);
    mgr.getConnection();
    fail("Should throw an exception when can't start TLS.");
  }

  @Test(expected = RuntimeException.class)
  public void testConnectionRuntimeFailPostConnectDisconnect() throws Exception {
    config.setSecureConnection(true);
    config.setTLS(true);

    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.startTLS();
    expectLastCall().andThrow(new RuntimeException());

    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());

    replay(conn);
    mgr.getConnection();
    fail("Should throw an exception when can't start TLS.");
  }

  @Test
  public void testBoundConnection() throws Exception {
    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.bind(anyInt(), isA(String.class), isA(byte[].class));
    replay(conn);

    mgr.getBoundConnection("dn=people", "password");
  }

  @Test(expected = LdapException.class)
  public void testBoundConnectionCantBind() throws Exception {
    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.bind(anyInt(), isA(String.class), isA(byte[].class));
    expectLastCall().andThrow(new LDAPException());
    replay(conn);

    mgr.getBoundConnection("dn=people", "password");
    fail("Should throw an exception when bind throws an exception.");
  }

  @Test
  public void testConnectionAutoBind() throws Exception {
    conn.setConstraints(isA(LDAPConstraints.class));

    conn.connect(isA(String.class), anyInt());

    conn.bind(anyInt(), isA(String.class), isA(byte[].class));

    config.setAutoBind(true);
    mgr.init();

    replay(conn);

    mgr.getConnection();
  }

  @Test
  public void testInitKeystoreNoPassword() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setSecureConnection(true);
    mgr.init();
  }

  @Test
  public void testInitKeystorePassword() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setKeystorePassword(keystorePassword);
    config.setSecureConnection(true);
    mgr.init();
  }

  @Test(expected = LdapException.class)
  public void testInitKeystoreMissing() throws Exception {
    config.setKeystoreLocation(keystoreLocation + "xxx");
    config.setSecureConnection(true);
    mgr.init();
    fail("Should throw exception if the keystore location is invalid.");
  }

  @Test
  public void testConnectionSecureTls() throws Exception {
    config.setKeystoreLocation(keystoreLocation);
    config.setSecureConnection(true);
    config.setTLS(true);
    mgr.init();
    mgr.getConnection();
  }

  @Test
  public void testReturnNullConnection() throws Exception {
    mgr.returnConnection(null);
  }

  @Test
  public void testReturnLiveConnection() throws Exception {
    conn.disconnect();
    replay(conn);
    mgr.returnConnection(conn);
  }

  @Test
  public void testReturnBadConnection() throws Exception {
    conn.disconnect();
    expectLastCall().andThrow(new LDAPException());
    replay(conn);

    mgr.returnConnection(conn);
  }
}
