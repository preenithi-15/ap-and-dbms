"""Local minimized-feature comparison service for synthetic incidents."""
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
from statistics import mean

VERSION = "attack-dna-1.0"
WEIGHTS = {"sequence": .25, "timing": .20, "target": .15, "behavior": .25, "progression": .15}

def similarity(a, b):
    return 100.0 if a == b else (200.0 * len(set(a) & set(b)) / (len(set(a)) + len(set(b))) if a or b else 0.0)

def sequence_similarity(a, b):
    if not a and not b: return 0.0
    previous = [0] * (len(b) + 1)
    for x in a:
        current = [0]
        for j, y in enumerate(b, 1): current.append(previous[j-1] + 1 if x == y else max(previous[j], current[-1]))
        previous = current
    return 200.0 * previous[-1] / (len(a) + len(b))

def compare(payload):
    left, right = payload["featuresA"]["events"], payload["featuresB"]["events"]
    dimensions = []
    weights = payload.get("weights", WEIGHTS)
    for key, label in (("sequence", "Sequence"), ("timing", "Timing"), ("target", "Target"), ("behavior", "Behavior"), ("progression", "Progression")):
        if key == "timing":
            def gaps(rows):
                ts = [float(r["relativeSeconds"]) for r in rows]
                return [max(1, int(y-x)) for x,y in zip(ts,ts[1:])]
            ga, gb = gaps(left), gaps(right)
            if not ga or not gb:
                score = 0.0
            else:
                n=min(len(ga),len(gb)); ratios=[min(x,y)/max(x,y) for x,y in zip(ga[:n],gb[:n])]
                score=round(mean(ratios)*100,1)
            n=min(len(ga),len(gb));matched=[f"{n} relative interval(s) compared"];differing=[f"relative intervals {len(ga)} vs {len(gb)}"]
            matched_left=left[:n+1];matched_right=right[:n+1]
            explanation="Relative event intervals are compared; absolute timestamps are not shared."
        else:
            va=[str(r.get(key,"")) for r in left if r.get(key)]
            vb=[str(r.get(key,"")) for r in right if r.get(key)]
            score=round(sequence_similarity(va,vb),1) if key=="sequence" else round(similarity(va,vb),1)
            matched=sorted(set(va)&set(vb)); differing=sorted(set(va)^set(vb))[:8]
            matched_left=[r for r in left if str(r.get(key,"")) in matched];matched_right=[r for r in right if str(r.get(key,"")) in matched]
            explanation="Feature order is compared with normalized longest-common-subsequence alignment; a missing event can change the score." if key=="sequence" else f"Shared {key} features are compared as a set; common behavior does not establish a common actor."
        dimensions.append({"key":key,"label":label,"score":score,"weight":weights[key],"explanation":explanation,"matched":matched,"differing":differing,
            "matchedEvents":[{"id":r["id"],"evidenceRef":r["evidenceRef"],"type":r["type"]} for r in matched_left],
            "caseBMatchedEvents":[{"id":r["id"],"evidenceRef":r["evidenceRef"],"type":r["type"]} for r in matched_right],
            "missing":[f"Case A has {payload['featuresA'].get('missingCount', 0)} telemetry gap(s); case B has {payload['featuresB'].get('missingCount', 0)}. Missing telemetry is not evidence that activity did not occur."] if payload["featuresA"].get("missingCount",0) or payload["featuresB"].get("missingCount",0) else [],
            "alternatives":["Shared patterns can result from common administrative tools, routine workflows, or similar logging coverage; the match alone does not establish a common actor."]})
    overall=round(sum(d["score"]*d["weight"] for d in dimensions),1)
    return {"overall":overall,"methodVersion":VERSION,"caveat":"Similarity is a synthetic behavioral lead, not attribution or a calibrated probability.","dimensions":dimensions,
            "caseA":payload["featuresA"]["caseId"],"caseB":payload["featuresB"]["caseId"],"inputNotes":[payload["featuresA"]["coverageNote"],payload["featuresB"]["coverageNote"]]}

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path != "/compare": self.send_error(404); return
        try:
            n=int(self.headers.get("Content-Length","0"))
            if n>100_000: raise ValueError("Request too large")
            result=compare(json.loads(self.rfile.read(n)))
            body=json.dumps(result).encode();self.send_response(200);self.send_header("Content-Type","application/json");self.send_header("Content-Length",str(len(body)));self.end_headers();self.wfile.write(body)
        except Exception as exc:
            body=json.dumps({"error":str(exc)}).encode();self.send_response(400);self.send_header("Content-Type","application/json");self.send_header("Content-Length",str(len(body)));self.end_headers();self.wfile.write(body)
    def log_message(self, *_): pass

if __name__ == "__main__":
    print("Threat Trace analytics listening on http://localhost:8100")
    HTTPServer(("127.0.0.1",8100),Handler).serve_forever()
