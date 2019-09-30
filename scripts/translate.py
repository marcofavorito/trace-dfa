#!/usr/bin/env python3


from opyenxes.factory.XFactory import XFactory
from opyenxes.id.XIDFactory import XIDFactory
from opyenxes.data_out.XesXmlSerializer import XesXmlSerializer
import sys
import os

if __name__ == "__main__":
    filepath = sys.argv[1]
    outdir = sys.argv[2]

    traindir = os.path.join(outdir, "train")
    testdir = os.path.join(outdir, "test")

    fp = open(filepath)

    lines = [l for l in fp.readlines()]
    acceptances = []
    traces = []
    for l in lines:
        split = l.split("\t")
        acceptances.append(split[0].strip())
        traces.append(split[1].strip().split(";") if len(split) > 1 else [])

    positive_log = XFactory.create_log()
    negative_log = XFactory.create_log()

    for acc, t in zip(acceptances, traces):
        trace = XFactory.create_trace()
        for e in t:
            event = XFactory.create_event()
            attribute = XFactory.create_attribute_literal("concept:name", e)
            event.get_attributes()["string"] = attribute
            trace.append(event)
        if acc == "Y":
            positive_log.append(trace)
        else:
            negative_log.append(trace)

    with open(os.path.join(outdir, "T_OK.xes"), mode="w") as fout:
        XesXmlSerializer().serialize(positive_log, fout)
    with open(os.path.join(outdir, "T.xes"), mode="w") as fout:
        XesXmlSerializer().serialize(negative_log, fout)
