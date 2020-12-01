package won.auth.check;

import won.auth.model.*;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class TargetAtomCheck {
    private Set<URI> allowedSockets;
    private Set<URI> allowedSocketTypes;
    private Set<ConnectionState> allowedConnectionStates;

    public Set<URI> getAllowedSockets() {
        return allowedSockets;
    }

    public Set<URI> getAllowedSocketTypes() {
        return allowedSocketTypes;
    }

    public Set<ConnectionState> getAllowedConnectionStates() {
        return allowedConnectionStates;
    }

    public static TargetAtomCheck of(Collection<TreeExpression> pathToTargetAtom) {
        TargetAtomCheck check = new TargetAtomCheck();
        for (TreeExpression ex : pathToTargetAtom) {
            ex.accept(new TreeExpressionVisitor() {
                @Override
                public void visit(ConnectionExpression other) {
                    check.allowedConnectionStates = other.getConnectionStates();
                }

                @Override
                public void visit(SocketExpression other) {
                    check.allowedSocketTypes = other.getSocketTypes();
                    check.allowedSockets = other.getSocketIris();
                }

                @Override
                public void visit(ConnectionsExpression other) {
                    check.allowedConnectionStates = other.getConnectionStates();
                    if (check.allowedConnectionStates.isEmpty()) {
                        check.allowedConnectionStates = Collections.singleton(ConnectionState.CONNECTED);
                    }
                }
            });
        }
        return check;
    }
}
