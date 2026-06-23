# Graph Report - SMS-Budgeter  (2026-06-23)

## Corpus Check
- 29 files · ~13,314 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 11 nodes · 10 edges · 2 communities
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `b99bde8e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- [[_COMMUNITY_Community 0|Community 0]]
- [[_COMMUNITY_Community 1|Community 1]]

## God Nodes (most connected - your core abstractions)
1. `SMS Budget Tracker — Handoff Document` - 7 edges
2. `What Has Been Built So Far` - 4 edges
3. `Repository` - 1 edges
4. `Current Build Status` - 1 edges
5. `Architecture` - 1 edges
6. `Source Files (21 Kotlin files)` - 1 edges
7. `Implemented Features` - 1 edges
8. `How to Continue` - 1 edges
9. `Important Notes` - 1 edges
10. `Git History (Recent Merges)` - 1 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Import Cycles
- None detected.

## Communities (2 total, 0 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.29
Nodes (6): Current Build Status, Git History (Recent Merges), How to Continue, Important Notes, Repository, SMS Budget Tracker — Handoff Document

### Community 1 - "Community 1"
Cohesion: 0.50
Nodes (4): Architecture, Implemented Features, Source Files (21 Kotlin files), What Has Been Built So Far

## Knowledge Gaps
- **8 isolated node(s):** `Repository`, `Current Build Status`, `Architecture`, `Source Files (21 Kotlin files)`, `Implemented Features` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `SMS Budget Tracker — Handoff Document` connect `Community 0` to `Community 1`?**
  _High betweenness centrality (0.867) - this node is a cross-community bridge._
- **Why does `What Has Been Built So Far` connect `Community 1` to `Community 0`?**
  _High betweenness centrality (0.533) - this node is a cross-community bridge._
- **What connects `Repository`, `Current Build Status`, `Architecture` to the rest of the system?**
  _8 weakly-connected nodes found - possible documentation gaps or missing edges._