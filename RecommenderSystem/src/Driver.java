
public class Driver {

	public static void main(String[] args) throws Exception {
		/*
		DataDividerByUser dataDividerByUser = new DataDividerByUser();
		CoOccurrenceMatrixGenerator coOccurrenceMatrixGenerator = new CoOccurrenceMatrixGenerator();
		Multiplication multiplication = new Multiplication();
		RecommenderListGenerator generator = new RecommenderListGenerator();
		*/
		String[] path1 = {args[0], args[1]};
		String[] path2 = {args[1], args[2]};
		String[] path3 = {args[4], args[0], args[3]};
		String[] path4 = {args[5], args[6], args[3], args[7]};
		
		DataDividerByUser.main(path1);
		CoOccurrenceMatrixGenerator.main(path2);
		Multiplication.main(path3);
		RecommenderListGenerator.main(path4);
	}

}
