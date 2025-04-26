import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

public class ExampleUsage {
    public static void main(String[] args) {
        MongoDatabase database = MongoDBClient.getDatabase();
        MongoCollection<Document> collection = database.getCollection("test");
        for (Document doc : collection.find()) {
            System.out.println(doc.toJson());
        }
    }
}