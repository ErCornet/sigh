package norswap.sigh;

import norswap.autumn.Autumn;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.sigh.ast.*;
import java.text.ParseException;

import static java.util.Arrays.asList;

public class Expansion
{
    private static final SighGrammar grammar = new SighGrammar();

    private static final ParseOptions parseOptions = ParseOptions.builder().wellFormednessCheck(false).get();

    /**
     * Serializes node {@code n}
     */
    static String serialize(SighNode n) throws Exception
    {
        if(n instanceof FunDeclarationNode)
        {
            FunDeclarationNode n_ = (FunDeclarationNode) n;
            String[] paramsToString = new String[n_.parameters.size()];
            for(int i = 0; i < paramsToString.length; i++) paramsToString[i] = serialize(n_.parameters.get(i));
            return "fun " + n_.name + "(" + String.join(", ", paramsToString) + "): " + serialize(n_.returnType) + " " + serialize(n_.block) + ";";
        }
        else if(n instanceof ParameterNode)
        {
            ParameterNode n_ = (ParameterNode) n;
            return n_.name + ": " + serialize(n_.type);
        }
        else if(n instanceof TypeNode)
        {
            if(n instanceof SimpleTypeNode)
                return ((SimpleTypeNode) n).name;
            else if(n instanceof ArrayTypeNode)
                return serialize(((ArrayTypeNode) n).componentType) + "[]";
            else if(n instanceof FunTypeNode)
            {
                FunTypeNode n_ = (FunTypeNode) n;
                String[] paramsToString = new String[n_.parametersTypes.size()];
                for(int i = 0; i < paramsToString.length; i++) paramsToString[i] = serialize(n_.parametersTypes.get(i));
                return "<(" + String.join(", ", paramsToString) + "): " + serialize(n_.returnType) + ">";
            }
        }
        else if(n instanceof BlockNode)
        {
            BlockNode n_ = (BlockNode) n;
            String[] statementsToString = new String[n_.statements.size()];
            for(int i = 0; i < statementsToString.length; i++) statementsToString[i] = serialize(n_.statements.get(i));
            return "{ " + String.join("; ", statementsToString) + " }";
        }
        else if(n instanceof ReturnNode)
            return "return " + serialize(((ReturnNode) n).expression);
        else if(n instanceof ReferenceNode)
            return ((ReferenceNode) n).name;
        else if(n instanceof BinaryExpressionNode)
        {
            BinaryExpressionNode n_ = (BinaryExpressionNode) n;
            return serialize(n_.left) + n_.operator.string + serialize(n_.right);
        }

        throw new Exception("Serialization not yet implement for " + n);
    }

    /**
     * Expands FunDeclarationNode {@code input} to a lazy FunDeclarationNode.
     *
     * fun #f(#params): #retType #block
     *
     * expands to :
     *
     * fun #f(#params): <(): #retType> { fun _(): #retType #block return _ }
     */
    static public String lazy(String input) throws Exception
    {
        ParseResult result = Autumn.parse(grammar.root, input, parseOptions);
        if (!result.fullMatch) throw new RuntimeException(new ParseException(result.toString(), result.errorOffset));

        SighNode ast = result.topValue();

        FunDeclarationNode node = (FunDeclarationNode) ((RootNode) ast).statements.get(0);

        FunDeclarationNode lazyFun =
            new FunDeclarationNode(null, node.name,
                node.parameters,
                new FunTypeNode(null, asList(), node.returnType),
                new BlockNode(null,
                    asList(
                        new FunDeclarationNode(null, "_",
                            asList(),
                            node.returnType,
                            node.block),
                        new ReturnNode(null, new ReferenceNode(null, "_")))));

        // TODO Rename references from #node.name to "_" in #node.block

        return serialize(lazyFun);
    }
}
