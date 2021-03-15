package edu.cornell.cs.apl.viaduct.backend.aby

import de.tu_darmstadt.cs.encrypto.aby.ABYParty
import de.tu_darmstadt.cs.encrypto.aby.Aby
import de.tu_darmstadt.cs.encrypto.aby.Phase
import de.tu_darmstadt.cs.encrypto.aby.Role
import de.tu_darmstadt.cs.encrypto.aby.Share
import de.tu_darmstadt.cs.encrypto.aby.SharingType
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import java.util.Scanner
import kotlin.system.measureTimeMillis
import mu.KotlinLogging

private var logger = KotlinLogging.logger("ABYBenchRunner")

private typealias ABYBenchmark = (ABYParty, ABYCircuitBuilder) -> Unit

class ABYBenchRunner(
    val host: String,
    val hostAddress: Map<String, String>,
    val benchmark: String,
    val input: Scanner
) {
    private val benchmarks: Map<String, ABYBenchmark> = mapOf(
        "HistoricalMillionaires" to { aby, builder -> benchLANHistoricalMillionaires(aby, builder) },
        "Biomatch" to { aby, builder -> benchLANBiomatch(aby, builder) },
        "HHIScore" to { aby, builder -> benchLANHHIScore(aby, builder) },
        "Kmeans" to { aby, builder -> benchLANKmeans(aby, builder) },
        "Median" to { aby, builder -> benchLANMedian(aby, builder) },
        "TwoRoundBidding" to { aby, builder -> benchLANTwoRoundBidding(aby, builder) },
        "Conversions" to { aby, builder -> benchLANConversions(aby, builder) }
    )

    fun executeABYCircuit(aby: ABYParty) {
        val execDuration = measureTimeMillis { aby.execCircuit() }

        logger.info {
            "executed ABY circuit in ${execDuration}ms\n" +
                "total gates: ${aby.totalGates}\n" +
                "total depth: ${aby.totalDepth}\n" +
                "total time: ${aby.getTiming(Phase.P_TOTAL)}\n" +
                "total sent/recv: ${aby.getSentData(Phase.P_TOTAL)} / ${aby.getReceivedData(Phase.P_TOTAL)}\n" +
                "network time: ${aby.getTiming(Phase.P_NETWORK)}\n" +
                "setup time: ${aby.getTiming(Phase.P_SETUP)}\n" +
                "setup sent/recv: ${aby.getSentData(Phase.P_SETUP)} / ${aby.getReceivedData(Phase.P_SETUP)}\n" +
                "online time: ${aby.getTiming(Phase.P_ONLINE)}\n" +
                "online sent/recv: ${aby.getSentData(Phase.P_ONLINE)} / ${aby.getReceivedData(Phase.P_ONLINE)}\n"
        }
    }

    private fun benchLANBiomatch(aby: ABYParty, builder: ABYCircuitBuilder) {
        fun match_alice(db1: Int, db2: Int): Share {
            val tmp = builder.arithCircuit.putINGate(db1.toBigInteger(), BITLEN, builder.role)
            val tmp1 = builder.arithCircuit.putDummyINGate(BITLEN)
            val dist1 = builder.arithCircuit.putSUBGate(tmp, tmp1)

            val tmp3 = builder.arithCircuit.putINGate(db2.toBigInteger(), BITLEN, builder.role)
            val tmp4 = builder.arithCircuit.putDummyINGate(BITLEN)
            val dist2 = builder.arithCircuit.putSUBGate(tmp3, tmp4)

            val tmp8 = builder.arithCircuit.putMULGate(dist1, dist1)
            val tmp11 = builder.arithCircuit.putMULGate(dist2, dist2)
            val tmp12 = builder.arithCircuit.putADDGate(tmp8, tmp11)
            return builder.yaoCircuit.putA2YGate(tmp12)
        }

        fun match_bob(s1: Int, s2: Int): Share {
            val tmp = builder.arithCircuit.putDummyINGate(BITLEN)
            val tmp1 = builder.arithCircuit.putINGate(s1.toBigInteger(), BITLEN, builder.role)
            val dist1 = builder.arithCircuit.putSUBGate(tmp, tmp1)

            val tmp3 = builder.arithCircuit.putDummyINGate(BITLEN)
            val tmp4 = builder.arithCircuit.putINGate(s2.toBigInteger(), BITLEN, builder.role)
            val dist2 = builder.arithCircuit.putSUBGate(tmp3, tmp4)

            val tmp8 = builder.arithCircuit.putMULGate(dist1, dist1)
            val tmp11 = builder.arithCircuit.putMULGate(dist2, dist2)
            val tmp12 = builder.arithCircuit.putADDGate(tmp8, tmp11)
            return builder.yaoCircuit.putA2YGate(tmp12)
        }

        val n = 500
        val d = 4

        when (host) {
            "alice" -> {
                val a_db = Array<Int>(n * d) { 0 }
                var i = 0
                while (i < n * d) {
                    a_db[i] = input.nextInt()
                    i += 1
                }

                var min_dist = match_alice(a_db[0], a_db[1])
                var i_2 = 0
                while (i_2 < n) {
                    val db1 = a_db[i_2 * d]
                    val db2 = a_db[(i_2 * d) + 1]
                    val dist = match_alice(db1, db2)
                    val tmp50 = builder.yaoCircuit.putGTGate(min_dist, dist)
                    val mux = builder.yaoCircuit.putMUXGate(dist, min_dist, tmp50)
                    min_dist = mux
                    i_2 += 1
                }

                val out = builder.yaoCircuit.putOUTGate(min_dist, Role.ALL)
                executeABYCircuit(aby)
                logger.info { "alice out: ${out.clearValue32.toInt()}" }
            }

            "bob" -> {
                val b_sample = Array<Int>(d) { 0 }
                var i = 0
                while (i < d) {
                    b_sample[i] = input.nextInt()
                    i += 1
                }

                var min_dist = match_bob(b_sample[0], b_sample[1])
                var i_2 = 0
                while (i_2 < n) {
                    val s1 = b_sample[0]
                    val s2 = b_sample[1]
                    val dist = match_bob(s1, s2)
                    val tmp50 = builder.yaoCircuit.putGTGate(min_dist, dist)
                    val mux = builder.yaoCircuit.putMUXGate(dist, min_dist, tmp50)
                    min_dist = mux
                    i_2 += 1
                }

                val out = builder.yaoCircuit.putOUTGate(min_dist, Role.ALL)
                executeABYCircuit(aby)
                logger.info { "bob info: ${out.clearValue32.toInt()}" }
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    private fun benchLANHHIScore(aby: ABYParty, builder: ABYCircuitBuilder) {
        val storeCount = 500

        when (host) {
            "alice" -> {
                var a_rev = 0
                var i = 0
                while (i < storeCount) {
                    a_rev += input.nextInt()
                    i += 1
                }

                val tmp5 = builder.yaoCircuit.putINGate(a_rev.toBigInteger(), BITLEN, builder.role)
                val tmp6 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val total_market = builder.yaoCircuit.putADDGate(tmp5, tmp6)

                val tmp9 = builder.yaoCircuit.putINGate((100 * a_rev).toBigInteger(), BITLEN, builder.role)
                val a_share =
                    builder.arithCircuit.putB2AGate(
                        builder.boolCircuit.putY2BGate(
                            Aby.putInt32DIVGate(builder.yaoCircuit, total_market, tmp9)
                        )
                    )

                val tmp13 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val b_share =
                    builder.arithCircuit.putB2AGate(
                        builder.boolCircuit.putY2BGate(
                            Aby.putInt32DIVGate(builder.yaoCircuit, total_market, tmp13)
                        )
                    )

                val tmp18 = builder.arithCircuit.putMULGate(a_share, a_share)
                val tmp21 = builder.arithCircuit.putMULGate(b_share, b_share)
                val tmp22 = builder.arithCircuit.putADDGate(tmp18, tmp21)
                val out = builder.arithCircuit.putOUTGate(tmp22, Role.ALL)

                executeABYCircuit(aby)

                val res = out.clearValue32.toInt()

                logger.info { "alice out: $res" }
            }

            "bob" -> {
                var b_rev = 0
                var i = 0
                while (i < storeCount) {
                    b_rev += input.nextInt()
                    i += 1
                }

                val tmp5 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val tmp6 = builder.yaoCircuit.putINGate(b_rev.toBigInteger(), BITLEN, builder.role)
                val total_market = builder.yaoCircuit.putADDGate(tmp5, tmp6)

                val tmp9 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val a_share =
                    builder.arithCircuit.putB2AGate(
                        builder.boolCircuit.putY2BGate(
                            Aby.putInt32DIVGate(builder.yaoCircuit, tmp9, total_market)
                        )
                    )

                val tmp13 = builder.yaoCircuit.putINGate((100 * b_rev).toBigInteger(), BITLEN, builder.role)
                val b_share =
                    builder.arithCircuit.putB2AGate(
                        builder.boolCircuit.putY2BGate(
                            Aby.putInt32DIVGate(builder.yaoCircuit, tmp13, total_market)
                        )
                    )

                val tmp18 = builder.arithCircuit.putMULGate(a_share, a_share)
                val tmp21 = builder.arithCircuit.putMULGate(b_share, b_share)
                val tmp22 = builder.arithCircuit.putADDGate(tmp18, tmp21)
                val out = builder.arithCircuit.putOUTGate(tmp22, Role.ALL)

                executeABYCircuit(aby)

                val res = out.clearValue32.toInt()

                logger.info { "bob out: $res" }
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    private fun benchLANHistoricalMillionaires(aby: ABYParty, builder: ABYCircuitBuilder) {
        val length = 500

        when (host) {
            "alice" -> {
                var a_min: Int = 0
                for (i in 1..length) {
                    val inval = input.nextInt()
                    a_min = Integer.min(a_min, inval)
                }

                logger.info { "alice: received inputs" }

                val a_in = builder.yaoCircuit.putINGate(a_min.toBigInteger(), BITLEN, builder.role)
                val b_in = builder.yaoCircuit.putDummyINGate(BITLEN)
                val cmp = builder.yaoCircuit.putGTGate(a_in, b_in)
                val out = builder.yaoCircuit.putOUTGate(cmp, Role.ALL)
                executeABYCircuit(aby)
                logger.info { "alice out: ${out.clearValue32.toInt()}" }
            }

            "bob" -> {
                var b_min: Int = 0
                for (i in 1..length) {
                    val inval = input.nextInt()
                    b_min = Integer.min(b_min, inval)
                }

                logger.info { "bob: received inputs" }

                val a_in = builder.yaoCircuit.putDummyINGate(BITLEN)
                val b_in = builder.yaoCircuit.putINGate(b_min.toBigInteger(), BITLEN, builder.role)
                val cmp = builder.yaoCircuit.putGTGate(a_in, b_in)
                val out = builder.yaoCircuit.putOUTGate(cmp, Role.ALL)
                executeABYCircuit(aby)
                logger.info { "bob out: ${out.clearValue32.toInt()}" }
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    fun benchLANMedian(aby: ABYParty, builder: ABYCircuitBuilder) {
        val n = 200
        when (host) {
            "alice" -> {
                val adata = Array<Int>(n) { 0 }

                var i = 0
                while (i < n) {
                    adata[i] = input.nextInt()
                    i += 1
                }

                var cur_a = 0
                var iter = 0
                while (iter < n) {
                    val tmp13 = builder.yaoCircuit.putINGate(adata[cur_a].toBigInteger(), BITLEN, builder.role)
                    val tmp15 = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val tmp16 = builder.yaoCircuit.putNOTGate(builder.yaoCircuit.putGTGate(tmp13, tmp15))
                    val tmp17 = builder.yaoCircuit.putOUTGate(tmp16, Role.ALL)

                    executeABYCircuit(aby)

                    if (tmp17.clearValue32.toInt() == 1) {
                        cur_a += 1
                    } else {
                    }

                    aby.reset()
                    iter += 1
                }

                var median: Int
                val tmp19 = builder.yaoCircuit.putINGate(adata[cur_a].toBigInteger(), BITLEN, builder.role)
                val tmp21 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val tmp22 = builder.yaoCircuit.putNOTGate(builder.yaoCircuit.putGTGate(tmp19, tmp21))
                val tmp23 = builder.yaoCircuit.putOUTGate(tmp22, Role.ALL)

                executeABYCircuit(aby)

                // TODO: need to send cleartext value of median to other host
                if (tmp23.clearValue32.toInt() == 1) {
                    median = adata[cur_a]
                    logger.info { "median: $median" }
                } else {
                }

                aby.reset()
            }

            "bob" -> {
                val bdata = Array<Int>(n) { 0 }

                var i = 0
                while (i < n) {
                    bdata[i] = input.nextInt()
                    i += 1
                }

                var cur_b = 0
                var iter = 0
                while (iter < n) {
                    val tmp13 = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val tmp15 = builder.yaoCircuit.putINGate(bdata[cur_b].toBigInteger(), BITLEN, builder.role)
                    val tmp16 = builder.yaoCircuit.putNOTGate(builder.yaoCircuit.putGTGate(tmp13, tmp15))
                    val tmp17 = builder.yaoCircuit.putOUTGate(tmp16, Role.ALL)

                    executeABYCircuit(aby)

                    if (tmp17.clearValue32.toInt() == 1) {
                    } else {
                        cur_b += 1
                    }

                    aby.reset()
                    iter += 1
                }

                var median: Int
                val tmp19 = builder.yaoCircuit.putDummyINGate(BITLEN)
                val tmp21 = builder.yaoCircuit.putINGate(bdata[cur_b].toBigInteger(), BITLEN, builder.role)
                val tmp22 = builder.yaoCircuit.putNOTGate(builder.yaoCircuit.putGTGate(tmp19, tmp21))
                val tmp23 = builder.yaoCircuit.putOUTGate(tmp22, Role.ALL)

                executeABYCircuit(aby)

                // TODO: need to send cleartext value of median to other host
                if (tmp23.clearValue32.toInt() == 1) {
                } else {
                    median = bdata[cur_b]
                    logger.info { "median: $median" }
                }

                aby.reset()
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    fun benchLANTwoRoundBidding(aby: ABYParty, builder: ABYCircuitBuilder) {
        val n = 500
        when (host) {
            "alice" -> {
                val abids1 = Array<Int>(n) { 0 }
                val abids2 = Array<Int>(n) { 0 }

                var i = 0
                while (i < n) {
                    abids1[i] = input.nextInt()
                    i += 1
                }

                var i_1 = 0
                while (i_1 < n) {
                    val tmp15 = builder.yaoCircuit.putINGate(abids1[i_1].toBigInteger(), BITLEN, builder.role)
                    val tmp17 = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val tmp18 = builder.yaoCircuit.putGTGate(tmp17, tmp15)
                    val tmp19 = builder.yaoCircuit.putOUTGate(tmp18, Role.ALL)

                    executeABYCircuit(aby)

                    val winner = tmp19.clearValue32.toInt()

                    aby.reset()

                    logger.info { "alice out: $winner" }
                    logger.info { "bob out: $winner" }

                    i_1 += 1
                }

                var i_2 = 0
                while (i_2 < n) {
                    abids1[i_2] = input.nextInt()
                    i_2 += 1
                }

                var i_3 = 0
                while (i_3 < n) {
                    val abid = builder.yaoCircuit.putINGate(((abids1[i_3] + abids2[i_3]) / 2).toBigInteger(), BITLEN, builder.role)
                    val bbid = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val tmp46 = builder.yaoCircuit.putGTGate(bbid, abid)
                    val tmp47 = builder.yaoCircuit.putOUTGate(tmp46, Role.ALL)

                    executeABYCircuit(aby)

                    val winner_1 = tmp47.clearValue32.toInt()

                    aby.reset()

                    logger.info { "alice out: $winner_1" }
                    logger.info { "bob out: $winner_1" }

                    i_3 += 1
                }
            }

            "bob" -> {
                val bbids1 = Array<Int>(n) { 0 }
                val bbids2 = Array<Int>(n) { 0 }

                var i = 0
                while (i < n) {
                    bbids1[i] = input.nextInt()
                    i += 1
                }

                var i_1 = 0
                while (i_1 < n) {
                    val tmp15 = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val tmp17 = builder.yaoCircuit.putINGate(bbids1[i_1].toBigInteger(), BITLEN, builder.role)
                    val tmp18 = builder.yaoCircuit.putGTGate(tmp17, tmp15)
                    val tmp19 = builder.yaoCircuit.putOUTGate(tmp18, Role.ALL)

                    executeABYCircuit(aby)

                    val winner = tmp19.clearValue32.toInt()

                    aby.reset()

                    logger.info { "alice out: $winner" }
                    logger.info { "bob out: $winner" }

                    i_1 += 1
                }

                var i_2 = 0
                while (i_2 < n) {
                    bbids1[i_2] = input.nextInt()
                    i_2 += 1
                }

                var i_3 = 0
                while (i_3 < n) {
                    val abid = builder.yaoCircuit.putDummyINGate(BITLEN)
                    val bbid = builder.yaoCircuit.putINGate(((bbids1[i_3] + bbids2[i_3]) / 2).toBigInteger(), BITLEN, builder.role)
                    val tmp46 = builder.yaoCircuit.putGTGate(bbid, abid)
                    val tmp47 = builder.yaoCircuit.putOUTGate(tmp46, Role.ALL)

                    executeABYCircuit(aby)

                    val winner_1 = tmp47.clearValue32.toInt()

                    aby.reset()

                    logger.info { "alice out: $winner_1" }
                    logger.info { "bob out: $winner_1" }

                    i_3 += 1
                }
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    fun benchLANKmeans(aby: ABYParty, builder: ABYCircuitBuilder) {
        val a_len = 50
        val b_len = 50
        val len = a_len + b_len
        val dim = 2
        val num_clusters = 4
        val num_iterations = 3

        // YaoABY
        val data = Array<Share?>(len * dim) { null }

        when (host) {
            "alice" -> {
                var i = 0
                while (i < a_len * dim) {
                    val x = input.nextInt()
                    data[i] = builder.yaoCircuit.putINGate(x.toBigInteger(), BITLEN, builder.role)
                    i += 1
                }

                var i_1 = 0
                while (i_1 < b_len * dim) {
                    data[(a_len * dim) + i_1] = builder.yaoCircuit.putDummyINGate(BITLEN)
                    i_1 += 1
                }
            }

            "bob" -> {
                var i = 0
                while (i < a_len * dim) {
                    data[i] = builder.yaoCircuit.putDummyINGate(BITLEN)
                    i += 1
                }

                var i_1 = 0
                while (i_1 < b_len * dim) {
                    val x = input.nextInt()
                    data[(a_len * dim) + i_1] = builder.yaoCircuit.putINGate(x.toBigInteger(), BITLEN, builder.role)
                    i_1 += 1
                }
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }

        // ArithABY
        val clusters = Array<Share?>(num_clusters * dim) { null }
        val stride = len / num_clusters

        var c = 0
        while (c < num_clusters) {
            var d = 0
            while (d < dim) {
                clusters[(c * dim) + d] =
                    builder.arithCircuit.putY2AGate(data[(stride * c * dim) + d], builder.boolCircuit)
                d += 1
            }
            c += 1
        }

        var iter = 0
        while (iter < num_iterations) {
            // YaoABY
            val best_clusters = Array<Share?>(len) { null }

            // assignment phase
            var i = 0
            while (i < len) {
                var best_dist = builder.arithCircuit.putCONSGate(0.toBigInteger(), BITLEN)
                var best_cluster = builder.yaoCircuit.putCONSGate(0.toBigInteger(), BITLEN)

                // initialize point to first cluster
                var d = 0
                while (d < dim) {
                    val tmp62 =
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putY2BGate(data[(i * dim) + d])
                        )
                    val sub = builder.arithCircuit.putSUBGate(tmp62, clusters[d])
                    val tmp68 = builder.arithCircuit.putMULGate(sub, sub)
                    best_dist = builder.arithCircuit.putADDGate(best_dist, tmp68)

                    d += 1
                }

                // assign point to nearest cluster
                var c2 = 1
                while (c2 < num_clusters) {
                    var dist = builder.arithCircuit.putCONSGate(0.toBigInteger(), BITLEN)
                    var d2 = 0
                    while (d2 < dim) {
                        val tmp80 =
                            builder.arithCircuit.putB2AGate(
                                builder.boolCircuit.putY2BGate(data[(i * dim) + d2])
                            )
                        val sub = builder.arithCircuit.putSUBGate(tmp80, clusters[(c2 * dim) + d2])
                        val tmp90 = builder.arithCircuit.putMULGate(sub, sub)
                        dist = builder.arithCircuit.putADDGate(dist, tmp90)
                        d2 += 1
                    }

                    val tmp91 = builder.yaoCircuit.putA2YGate(dist)
                    val tmp92 = builder.yaoCircuit.putA2YGate(best_dist)
                    val tmp93 = builder.yaoCircuit.putGTGate(tmp92, tmp91)
                    val tmp94 = builder.yaoCircuit.putCONSGate(c2.toBigInteger(), BITLEN)
                    val tmp96 = builder.yaoCircuit.putMUXGate(tmp94, best_cluster, tmp93)
                    best_cluster = tmp96
                    c2 += 1
                }

                best_clusters[i] = best_cluster
                i += 1
            }

            // update phase
            var c3 = 0
            while (c3 < num_clusters) {
                // YaoABY
                val new_centroid_sum = Array<Share?>(dim) { builder.yaoCircuit.putCONSGate(0.toBigInteger(), BITLEN) }
                var num_points = builder.yaoCircuit.putCONSGate(0.toBigInteger(), BITLEN)
                var i2 = 0
                while (i2 < len) {
                    val tmp108 = builder.yaoCircuit.putCONSGate(c3.toBigInteger(), BITLEN)
                    val in_cluster = builder.yaoCircuit.putEQGate(best_clusters[i2], tmp108)
                    var d3 = 0
                    while (d3 < dim) {
                        val tmp121 =
                            builder.yaoCircuit.putMUXGate(
                                data[(i2 * dim) + d3],
                                builder.yaoCircuit.putCONSGate(0.toBigInteger(), BITLEN),
                                in_cluster
                            )

                        new_centroid_sum[d3] = builder.yaoCircuit.putADDGate(new_centroid_sum[d3], tmp121)
                        d3 += 1
                    }

                    val op =
                        builder.yaoCircuit.putADDGate(
                            num_points,
                            builder.yaoCircuit.putCONSGate(1.toBigInteger(), BITLEN)
                        )
                    val mux = builder.yaoCircuit.putMUXGate(op, num_points, in_cluster)
                    num_points = mux
                    i2 += 1
                }

                var d4 = 0
                while (d4 < dim) {
                    val tmp132 =
                        builder.yaoCircuit.putGTGate(
                            num_points,
                            builder.yaoCircuit.putCONSGate(0.toBigInteger(), BITLEN)
                        )

                    val tmp136 = Aby.putInt32DIVGate(builder.yaoCircuit, num_points, new_centroid_sum[d4])
                    val tmp142 = builder.yaoCircuit.putA2YGate(clusters[(c3 * dim) + d4])

                    clusters[(c3 * dim) + d4] =
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putY2BGate(
                                builder.yaoCircuit.putMUXGate(tmp136, tmp142, tmp132)
                            )
                        )

                    d4 += 1
                }

                c3 += 1
            }

            iter += 1
        }

        var h = 0
        var out_gates = Array<Share?>(num_clusters * dim) {
            builder.arithCircuit.putCONSGate(0.toBigInteger(), BITLEN)
        }
        while (h < num_clusters * dim) {
            out_gates[h] = builder.arithCircuit.putOUTGate(clusters[h], Role.ALL)
            h += 1
        }

        executeABYCircuit(aby)

        var i = 0
        while (i < num_clusters * dim) {
            logger.info { "out: ${out_gates[i]!!.clearValue32.toInt()}" }
            i += 1
        }
    }

    fun benchLANConversions(aby: ABYParty, builder: ABYCircuitBuilder) {
        when (host) {
            "alice" -> {
                val a2bAlice =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putA2BGate(
                            builder.arithCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role),
                            builder.yaoCircuit
                        ),
                        Role.ALL
                    )

                val a2bBob =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putA2BGate(
                            builder.arithCircuit.putDummyINGate(BITLEN),
                            builder.yaoCircuit
                        ),
                        Role.ALL
                    )

                val a2yAlice =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putA2YGate(
                            builder.arithCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val a2yBob =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putA2YGate(
                            builder.arithCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val b2aAlice =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val b2aBob =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val b2yAlice =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putB2YGate(
                            builder.boolCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val b2yBob =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putB2YGate(
                            builder.boolCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val y2aAlice =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putY2AGate(
                            builder.yaoCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role),
                            builder.boolCircuit
                        ),
                        Role.ALL
                    )

                val y2aBob =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putY2AGate(
                            builder.yaoCircuit.putDummyINGate(BITLEN),
                            builder.boolCircuit
                        ),
                        Role.ALL
                    )

                val y2bAlice =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putY2BGate(
                            builder.yaoCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val y2bBob =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putY2BGate(
                            builder.yaoCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                // check if subtraction to negative numbers work
                val sub =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putMULGate(
                            builder.arithCircuit.putSUBGate(
                                builder.arithCircuit.putCONSGate(2.toBigInteger(), BITLEN),
                                builder.arithCircuit.putCONSGate(5.toBigInteger(), BITLEN)
                            ),
                            builder.arithCircuit.putSUBGate(
                                builder.arithCircuit.putCONSGate(2.toBigInteger(), BITLEN),
                                builder.arithCircuit.putCONSGate(5.toBigInteger(), BITLEN)
                            )
                        ),
                        Role.ALL
                    )

                executeABYCircuit(aby)

                println("A2B Alice: ${a2bAlice.clearValue32.toInt()}")
                println("A2B Bob: ${a2bBob.clearValue32.toInt()}")

                println("A2Y Alice: ${a2yAlice.clearValue32.toInt()}")
                println("A2Y Bob: ${a2yBob.clearValue32.toInt()}")

                println("B2A Alice: ${b2aAlice.clearValue32.toInt()}")
                println("B2A Bob: ${b2aBob.clearValue32.toInt()}")

                println("B2Y Alice: ${b2yAlice.clearValue32.toInt()}")
                println("B2Y Bob: ${b2yBob.clearValue32.toInt()}")

                println("Y2A Alice: ${y2aAlice.clearValue32.toInt()}")
                println("Y2A Bob: ${y2aBob.clearValue32.toInt()}")

                println("Y2B Alice: ${y2bAlice.clearValue32.toInt()}")
                println("Y2B Bob: ${y2bBob.clearValue32.toInt()}")

                println("sub: ${sub.clearValue32.toInt()}")
            }

            "bob" -> {
                val a2bAlice =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putA2BGate(
                            builder.arithCircuit.putDummyINGate(BITLEN),
                            builder.yaoCircuit
                        ),
                        Role.ALL
                    )

                val a2bBob =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putA2BGate(
                            builder.arithCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role),
                            builder.yaoCircuit
                        ),
                        Role.ALL
                    )

                val a2yAlice =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putA2YGate(
                            builder.arithCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val a2yBob =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putA2YGate(
                            builder.arithCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val b2aAlice =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val b2aBob =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putB2AGate(
                            builder.boolCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val b2yAlice =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putB2YGate(
                            builder.boolCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val b2yBob =
                    builder.yaoCircuit.putOUTGate(
                        builder.yaoCircuit.putB2YGate(
                            builder.boolCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                val y2aAlice =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putY2AGate(
                            builder.yaoCircuit.putDummyINGate(BITLEN),
                            builder.boolCircuit
                        ),
                        Role.ALL
                    )

                val y2aBob =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putY2AGate(
                            builder.yaoCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role),
                            builder.boolCircuit
                        ),
                        Role.ALL
                    )

                val y2bAlice =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putY2BGate(
                            builder.yaoCircuit.putDummyINGate(BITLEN)
                        ),
                        Role.ALL
                    )

                val y2bBob =
                    builder.boolCircuit.putOUTGate(
                        builder.boolCircuit.putY2BGate(
                            builder.yaoCircuit.putINGate(input.nextInt().toBigInteger(), BITLEN, builder.role)
                        ),
                        Role.ALL
                    )

                // check if subtraction to negative numbers work
                val sub =
                    builder.arithCircuit.putOUTGate(
                        builder.arithCircuit.putMULGate(
                            builder.arithCircuit.putSUBGate(
                                builder.arithCircuit.putCONSGate(2.toBigInteger(), BITLEN),
                                builder.arithCircuit.putCONSGate(5.toBigInteger(), BITLEN)
                            ),
                            builder.arithCircuit.putSUBGate(
                                builder.arithCircuit.putCONSGate(2.toBigInteger(), BITLEN),
                                builder.arithCircuit.putCONSGate(5.toBigInteger(), BITLEN)
                            )
                        ),
                        Role.ALL
                    )

                executeABYCircuit(aby)

                println("A2B Alice: ${a2bAlice.clearValue32.toInt()}")
                println("A2B Bob: ${a2bBob.clearValue32.toInt()}")

                println("A2Y Alice: ${a2yAlice.clearValue32.toInt()}")
                println("A2Y Bob: ${a2yBob.clearValue32.toInt()}")

                println("B2A Alice: ${b2aAlice.clearValue32.toInt()}")
                println("B2A Bob: ${b2aBob.clearValue32.toInt()}")

                println("B2Y Alice: ${b2yAlice.clearValue32.toInt()}")
                println("B2Y Bob: ${b2yBob.clearValue32.toInt()}")

                println("Y2A Alice: ${y2aAlice.clearValue32.toInt()}")
                println("Y2A Bob: ${y2aBob.clearValue32.toInt()}")

                println("Y2B Alice: ${y2bAlice.clearValue32.toInt()}")
                println("Y2B Bob: ${y2bBob.clearValue32.toInt()}")

                println("sub: ${sub.clearValue32.toInt()}")
            }

            else -> throw ViaductInterpreterError("unknown host: $host")
        }
    }

    fun run() {
        val otherHost = if (host == "alice") "bob" else "alice"
        val role = if (host == "alice") Role.SERVER else Role.CLIENT
        val address = if (role == Role.SERVER) "" else hostAddress[otherHost] ?: DEFAULT_ADDRESS
        val aby = ABYParty(role, address, DEFAULT_PORT, Aby.getLT(), BITLEN)

        logger.info { "connected ABY to other host at $address:$DEFAULT_PORT" }

        val circuitBuilder =
            ABYCircuitBuilder(
                arithCircuit = aby.getCircuitBuilder(SharingType.S_ARITH)!!,
                boolCircuit = aby.getCircuitBuilder(SharingType.S_BOOL)!!,
                yaoCircuit = aby.getCircuitBuilder(SharingType.S_YAO)!!,
                bitlen = BITLEN,
                role = role
            )

        benchmarks[benchmark]!!(aby, circuitBuilder)
    }

    companion object {
        private const val DEFAULT_ADDRESS = "127.0.0.1"
        private const val DEFAULT_PORT = 7766
        private const val BITLEN: Long = 32
    }
}
