package org.freakz.common.net;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

public final class PublicUrlPolicy {

  private PublicUrlPolicy() {}

  static InetAddress[] resolve(URI uri) throws UnknownHostException {
    if (uri == null || uri.getScheme() == null || uri.getHost() == null || uri.getRawUserInfo() != null) {
      throw new UnknownHostException("Invalid public destination");
    }
    String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
    if (!"http".equals(scheme) && !"https".equals(scheme)) {
      throw new UnknownHostException("Invalid public URL scheme");
    }
    InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
    for (InetAddress address : addresses) {
      if (isPrivateOrLocal(address)) {
        throw new UnknownHostException("Destination does not resolve exclusively to public addresses");
      }
    }
    return addresses;
  }

  public static boolean isAllowed(URI uri) {
    try {
      resolve(uri);
      return true;
    } catch (UnknownHostException e) {
      return false;
    }
  }

  private static boolean isPrivateOrLocal(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return true;
    }
    if (address instanceof Inet4Address) {
      return isReservedIpv4(address.getAddress());
    }
    if (address instanceof Inet6Address) {
      byte first = address.getAddress()[0];
      return (first & 0xfe) == 0xfc;
    }
    return true;
  }

  private static boolean isReservedIpv4(byte[] address) {
    int first = Byte.toUnsignedInt(address[0]);
    int second = Byte.toUnsignedInt(address[1]);
    int third = Byte.toUnsignedInt(address[2]);

    return first == 0
        || first == 10
        || first == 127
        || (first == 100 && second >= 64 && second <= 127)
        || (first == 169 && second == 254)
        || (first == 172 && second >= 16 && second <= 31)
        || (first == 192 && second == 0 && third == 0)
        || (first == 192 && second == 0 && third == 2)
        || (first == 192 && second == 88 && third == 99)
        || (first == 192 && second == 168)
        || (first == 198 && (second == 18 || second == 19))
        || (first == 198 && second == 51 && third == 100)
        || (first == 203 && second == 0 && third == 113)
        || first >= 224;
  }
}
