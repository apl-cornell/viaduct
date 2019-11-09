package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.imp.protocols.ControlProtocol;
import edu.cornell.cs.apl.viaduct.imp.protocols.IdealFunctionality;
import edu.cornell.cs.apl.viaduct.imp.transformers.AnfConverter;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import edu.cornell.cs.apl.viaduct.imp.transformers.ImpPdgBuilderPreprocessor;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpProtocolInstantiationVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.io.IOException;
import java.io.PrintStream;

@Command(
    name = "specification",
    description = "Generate ideal functionality from specification program")
public class SpecificationCommand extends BaseCommand {

  @Override
  public void run() throws IOException {
    final ProgramNode program = this.input.parse();

    HostName idealFunctionality = HostName.create("idealFunctionality");
    HostName simulator = HostName.create("simulator");

    final Label corruptionLabel = Label.create(Principal.create("A"));
    final HostTrustConfiguration hostConfig =
        HostTrustConfiguration.builder()
        .add(
            HostDeclarationNode.builder()
            .setName(idealFunctionality)
            .setTrust(Label.strongest())
            .build())
        .add(
            HostDeclarationNode.builder()
            .setName(simulator)
            .setTrust(corruptionLabel)
            .build())
        .build();

    TypeChecker.run(program);

    final ProgramNode processedProgram =
        ImpPdgBuilderPreprocessor.run(AnfConverter.run(Elaborator.run(program)));

    // Check information flow and inject trust labels into the AST.
    InformationFlowChecker.run(processedProgram);

    final StatementNode main = processedProgram.processes().get(ProcessName.getMain()).getBody();

    // Generate program dependency graph.
    final ProgramDependencyGraph<ImpAstNode> pdg = new ImpPdgBuilderVisitor().generatePDG(main);

    // all data and computation is in the IdealFunctionality protocol
    final Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap = HashMap.empty();
    for (PdgNode<ImpAstNode> node : pdg.getOrderedNodes()) {
      if (node.isControlNode()) {
        protocolMap.put(node, ControlProtocol.getInstance());

      } else {
        final IdealFunctionality idealProtocol =
            new IdealFunctionality(idealFunctionality, simulator, corruptionLabel);

        protocolMap.put(node, idealProtocol);
      }
    }

    final ProgramNode generatedProgram =
        new ImpProtocolInstantiationVisitor(
                hostConfig, null, pdg, protocolMap, main)
            .run();

    try (PrintStream writer = output.newOutputStream("")) {
      Printer.run(generatedProgram, writer);
    }
  }
}
