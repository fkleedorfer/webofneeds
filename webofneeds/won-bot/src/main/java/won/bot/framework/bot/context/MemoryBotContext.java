package won.bot.framework.bot.context;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In memory context implementation using nested maps. This is the default implementation of the bot context.
 */
public class MemoryBotContext implements BotContext
{
  private Map<String, Map<String, Object>> contextMap = new HashMap<>();
  private Set<URI> nodeUris = new HashSet<>();
  private Map<String, List<URI>> namedNeedUriLists = new HashMap();

  @Override
  public Set<URI> retrieveAllNeedUris() {
    Set<URI> ret = new HashSet<>();
    ret.addAll(namedNeedUriLists.values().stream().flatMap(List::stream).collect(Collectors.toSet()));
    return ret;
  }

  @Override
  public synchronized boolean isNeedKnown(final URI needURI) {
    return retrieveAllNeedUris().contains(needURI);
  }

  @Override
  public synchronized void removeNeedUriFromNamedNeedUriList(URI uri, String name) {
    List<URI> uris = namedNeedUriLists.get(name);
    uris.remove(uri);
  }

  @Override
  public synchronized void appendToNamedNeedUriList(final URI uri, final String name) {

    List<URI> uris = this.namedNeedUriLists.get(name);
    if (uris == null) {
      uris = new ArrayList();
    }
    uris.add(uri);
    this.namedNeedUriLists.put(name, uris);
  }

  @Override
  public synchronized List<URI> getNamedNeedUriList(final String name) {

    List<URI> ret = new LinkedList<>();
    List<URI> namedList = this.namedNeedUriLists.get(name);
    if (namedList != null) {
      ret.addAll(namedList);
    }

    return ret;
  }

  @Override
  public synchronized boolean isNodeKnown(final URI wonNodeURI) {
    return nodeUris.contains(wonNodeURI);
  }


  @Override
  public synchronized void rememberNodeUri(final URI uri) {
    nodeUris.add(uri);
  }

  @Override
  public synchronized void removeNodeUri(final URI uri) {
    nodeUris.remove(uri);
  }

  private Map<String, Object> getCollection(String collectionName) {

    Map<String, Object> collection = contextMap.get(collectionName);
    if (collection == null) {
      collection = new HashMap<>();
      contextMap.put(collectionName, collection);
    }
    return collection;
  }

  @Override
  public synchronized void putGeneric(String collectionName, String key, final Serializable value) {
    getCollection(collectionName).put(key, value);
  }

  @Override
  public synchronized final Object getGeneric(String collectionName, String key) {
    return getCollection(collectionName).get(key);
  }

  @Override
  public synchronized final void removeGeneric(String collectionName, String key) {
    getCollection(collectionName).remove(key);
  }

  @Override
  public synchronized Collection<Object> genericValues(String collectionName) {

    Set<Object> set = new HashSet<>();
    set.addAll(getCollection(collectionName).values());
    return set;
  }
}
