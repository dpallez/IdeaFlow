package org.ideaflow.core;
import java.util.List;import org.ideaflow.api.*;import org.ideaflow.spi.*;
public final class DefaultDominanceStrategy implements org.ideaflow.spi.DominanceComparator{
 public String id(){return "pareto.deb-constraints";}public String displayName(){return "Pareto dominance with Deb constraints";}public CapabilityDescriptor capabilities(){return CapabilityDescriptor.general();}public int compare(Candidate a,Candidate b,List<ObjectiveDefinition> objectives){return ParetoDominance.compare(a,b,objectives);}
}
