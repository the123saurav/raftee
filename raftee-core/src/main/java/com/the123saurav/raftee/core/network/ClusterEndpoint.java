package com.the123saurav.raftee.core.network;

import java.net.URI;

/**
 * This class maintains current cluster endpoints and a connection to them.
 */
public record ClusterEndpoint(URI endpoint) {
}
