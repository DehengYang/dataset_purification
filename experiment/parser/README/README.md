1) create a new py file: RQ3_parser.py

read yaml file in results/ as a dict for each;

Refer to: utils.py read_yaml()

2) collect 
#1 7 reduced chunks -> original; purifiy
#2 6 reduced lines ->
ori_patch_lines
delta_patch_lines
#3 
3 test (cases):
  failing test cases:

3) write to excel

import xlsxwriter
def init_fl_sheet(xlsx_path):
    """
    init sheet
    """
    # Create a workbook and add a worksheet.
    workbook = xlsxwriter.Workbook(xlsx_path)
    worksheet = workbook.add_worksheet("sheet")

    # Add a bold format to use to highlight cells.
    title_f = workbook.add_format()
    title_f.set_font_size(10)
    title_f.set_align('center')
    title_f.set_align('vcenter')
    title_f.set_bold(True)
    title_f.set_bg_color('orange')
    title_f.set_font('Times New Roman')

    cell_f = workbook.add_format()
    cell_f.set_font_size(10)
    cell_f.set_align('center')
    cell_f.set_align('vcenter')
    cell_f.set_bold(False)
    cell_f.set_font('Times New Roman')
    cell_f.set_text_wrap(True)

    worksheet.write(0, 0,'bug_name',title_f)
    worksheet.write(0, 1,'if_done',title_f)
    worksheet.write(0, 2,'time_cost',title_f)
    worksheet.write(0, 3,'dir_exists',title_f)

    cnt = 4
    worksheet.write(0, cnt,'time_cost_fl', title_f)
    cnt += 1
    worksheet.write(0, cnt,'unit_tests', title_f)
    cnt += 1
    worksheet.write(0, cnt,'test_classes', title_f)
    cnt += 1
    worksheet.write(0, cnt,'total_stmts', title_f)
    cnt += 1
    worksheet.write(0, cnt,'total_pos_stmts', title_f)
    cnt += 1
    worksheet.write(0, cnt,'extra_failed_cases_fl', title_f)
    cnt += 1
    worksheet.write(0, cnt,'localized_stmts', title_f)
    cnt += 1
    worksheet.write(0, cnt,'unlocalized_stmts_size', title_f)
    cnt += 1
    worksheet.write(0, cnt,'fl_best_rank', title_f)
    cnt += 1

    worksheet.write(0, cnt,'time_cost_replicateTests', title_f)
    cnt += 1
    worksheet.write(0, cnt,'replicate_fake_tests', title_f)
    cnt += 1
    #worksheet.write(0, 4,'time_cost_classes_collection',title_f)
    # worksheet.write(0, 5,'time_cost_fl',title_f)
    # worksheet.write(0, 6,'time_cost_replicateTests',title_f)
    # worksheet.write(0, 7,'time_cost_whole_main',title_f)
    # worksheet.write(0, 8,'extra_failed_cases_fl',title_f)
    
    # worksheet.write(0, 9,'total_test_cases', title_f)
    # worksheet.write(0, 10,'total_stmts', title_f) 
    # worksheet.write(0, 11,'total_pos_stmts', title_f) 
    # worksheet.write(0, 12,'localized_stmts', title_f) 
    # worksheet.write(0, 13,'unlocalized_stmts_size', title_f) 
    # worksheet.write(0, 14,'fl_best_rank', title_f) 

    worksheet.set_column(0,0,10)
    worksheet.set_column(1,16,10)

    return workbook, worksheet, title_f, cell_f

#################################

worksheet.write(row_cnt, cnt, "{} & {}".format(buggyloc_size_cnt, related_buggyloc_size_cnt), cell_f)
    cnt += 1
    worksheet.write(row_cnt, cnt, "{} & {}".format(compiled_passed_cnt, compiled_failed_cnt), cell_f)
    cnt += 1
    worksheet.write(row_cnt, cnt, "{} & {}".format(failed_passed_cnt, failed_failed_cnt), cell_f)
    cnt += 1
    worksheet.write(row_cnt, cnt, "{} & {}".format(all_passed_cnt, all_failed_cnt), cell_f)
    cnt += 1
    worksheet.write(row_cnt, cnt, tc_replicate_before_pv, cell_f)
    cnt += 1
    worksheet.write(row_cnt, cnt, replicate_fake_test_methods_cnt, cell_f)
    cnt += 1


######################################


workbook, worksheet, title_f, cell_f = fl_func.init_fl_sheet(xlsx_path)
