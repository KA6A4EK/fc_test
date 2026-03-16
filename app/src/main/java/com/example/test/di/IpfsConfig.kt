package com.example.test.di

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Central config for IPFS/node connection (multiaddress, timeouts, test CID).
 * Ping timeout is shorter than fetch; fetch allows more time for block retrieval.
 */
object IpfsConfig {
    const val IPFS_MULTIADDRESS: String =
        "/dns4/ipfs.infra.cf.team/tcp/4001/p2p/12D3KooWKiqj21VphU2eE25438to5xeny6eP6d3PXT93ZczagPLT"

    /** CID used for ping (lightweight getBlocks call). From assignment. */
    const val PING_TEST_CID: String = "QmTBimFzPPP2QsB7TQGc2dr4BZD4i7Gm2X1mNtb6DqN9Dr"

    val fetchTimeout: Duration = 10.seconds
    val pingTimeout: Duration = 5.seconds
}
