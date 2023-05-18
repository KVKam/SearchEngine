package searchengine.services;

public interface IndexingService {

    Object startIndexing();

    Object stopIndexing();

    Object indexPade(String url);
}
