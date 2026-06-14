"""Tests for URL validation to prevent SSRF.

Tests _validate_url directly by copying the function to avoid Django import
dependencies. The function uses only stdlib modules (ipaddress, socket,
urllib.parse).
"""

import ipaddress
import socket
import unittest
import urllib.parse
from unittest.mock import patch


# Copy of _validate_url from util_views.py (avoids Django import chain)
def _validate_url(url):
    """Validate that a URL is safe to fetch server-side.

    Blocks requests to private/internal networks to prevent SSRF attacks.
    Only http and https schemes are allowed.

    Raises ValueError if the URL is not safe.
    """
    parsed = urllib.parse.urlparse(url)

    if parsed.scheme not in ("http", "https"):
        raise ValueError(f"URL scheme '{parsed.scheme}' is not allowed, only http/https")

    hostname = parsed.hostname
    if not hostname:
        raise ValueError("URL has no hostname")

    try:
        addrinfos = socket.getaddrinfo(hostname, parsed.port or (443 if parsed.scheme == "https" else 80))
    except socket.gaierror:
        raise ValueError(f"Cannot resolve hostname '{hostname}'")

    for family, _, _, _, sockaddr in addrinfos:
        ip = ipaddress.ip_address(sockaddr[0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
            raise ValueError(
                f"URL resolves to non-public address {ip} which is not allowed"
            )


class TestValidateUrl(unittest.TestCase):
    """Test _validate_url blocks SSRF vectors."""

    # --- Scheme checks ---

    def test_rejects_file_scheme(self):
        with self.assertRaises(ValueError):
            _validate_url("file:///etc/passwd")

    def test_rejects_ftp_scheme(self):
        with self.assertRaises(ValueError):
            _validate_url("ftp://internal-server/data")

    def test_rejects_gopher_scheme(self):
        with self.assertRaises(ValueError):
            _validate_url("gopher://internal:70/")

    def test_rejects_no_scheme(self):
        with self.assertRaises(ValueError):
            _validate_url("//no-scheme.example.com/path")

    def test_rejects_empty_hostname(self):
        with self.assertRaises(ValueError):
            _validate_url("http:///path")

    # --- Private/internal IP checks ---

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_loopback_ipv4(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("127.0.0.1", 80)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://localhost/latest/meta-data/")

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_loopback_ipv6(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (10, 1, 6, "", ("::1", 80, 0, 0)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://localhost/")

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_private_10(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("10.0.0.1", 80)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://internal.corp/")

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_private_172(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("172.16.0.1", 80)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://internal.corp/")

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_private_192(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("192.168.1.1", 80)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://internal.corp/")

    @patch("__main__.socket.getaddrinfo")
    def test_rejects_link_local_metadata(self, mock_getaddrinfo):
        """Block AWS/GCP/Azure instance metadata endpoint."""
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("169.254.169.254", 80)),
        ]
        with self.assertRaises(ValueError):
            _validate_url("http://169.254.169.254/latest/meta-data/")

    # --- Allowed URLs ---

    @patch("__main__.socket.getaddrinfo")
    def test_allows_public_ip(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("93.184.216.34", 443)),
        ]
        _validate_url("https://example.com/metrics")

    @patch("__main__.socket.getaddrinfo")
    def test_allows_http_scheme(self, mock_getaddrinfo):
        mock_getaddrinfo.return_value = [
            (2, 1, 6, "", ("93.184.216.34", 80)),
        ]
        _validate_url("http://example.com/metrics")

    # --- DNS resolution failure ---

    def test_rejects_unresolvable_hostname(self):
        with self.assertRaises(ValueError):
            _validate_url("http://this-hostname-does-not-exist-xyz123.invalid/")


if __name__ == "__main__":
    unittest.main()
