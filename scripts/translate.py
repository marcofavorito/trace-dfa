#!/usr/bin/env python3

from functools import reduce
from opyenxes.factory.XFactory import XFactory
from opyenxes.id.XIDFactory import XIDFactory
from opyenxes.data_out.XesXmlSerializer import XesXmlSerializer
import sys
import os
import shutil
from pathlib import Path
import logging

logging.basicConfig(level=logging.INFO)


if __name__ == "__main__":
    filepath = sys.argv[1]
    outdir = sys.argv[2]

    traindir = os.path.join(outdir, "train")
    testdir = os.path.join(outdir, "test")
    Path(testdir).mkdir(parents=True, exist_ok=True)
    Path(traindir).mkdir(parents=True, exist_ok=True)

    fp = open(filepath)

    lines = [l for l in fp.readlines()]
    acceptances = []
    traces = []
    for idx, l in enumerate(lines):
        split = l.strip().split("\t")
        acceptance = split[0].strip()
        acceptances.append(acceptance)
        
        trace = split[1].strip().split(";") if len(split) > 1 else []
        if trace != ['']:
            traces.append(trace)

        logging.info("trace {}, acc {}: {}".format(idx, acceptance, trace))

    logging.info("num traces: {}".format(len(traces)))
    logging.info("num acceptances: {}".format(len(acceptances)))
    logging.info("alphabet: {}".format(reduce(lambda x, y: x.union(y), map(set, traces))))

    positive_log = XFactory.create_log()
    negative_log = XFactory.create_log()

    assert len(acceptances) == len(traces)
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

    path_positives = os.path.join(outdir, "T_OK.xes")
    path_negatives = os.path.join(outdir, "T_OK.xes")
    with open(path_positives, mode="w") as fout:
        XesXmlSerializer().serialize(positive_log, fout)
    with open(path_negatives, mode="w") as fout:
        XesXmlSerializer().serialize(negative_log, fout)

    shutil.copy(path_positives, Path(traindir)) 
    shutil.copy(path_negatives, Path(traindir)) 
    
    shutil.copy(path_positives, Path(testdir)) 
    shutil.copy(path_negatives, Path(testdir)) 
